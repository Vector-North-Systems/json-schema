package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.vectornorthsystems.jsonschema.*
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.getQualifiedName
import com.vectornorthsystems.jsonschema.processor.node.*
import com.vectornorthsystems.jsonschema.processor.writer.StringProperties


/**
 * A visitor that walks the hierarchy and produces the tree of properties and types using [com.vectornorthsystems.jsonschema.processor.node.Node].
 */
internal class SchemaVisitor(
    val logger: KSPLogger,
) : KSDefaultVisitor<Node, Node>() {

    private companion object {
        /**
         *  How many objects deep a schema can nest, well beyond any real usable hierarchy but short of a stack overflow.
         */
        const val MAX_DEPTH = 32
    }

    /**
     * The objects being built, by type, to find types that contain themselves.
     */
    private val inProgress = mutableMapOf<String, ObjectNode>()

    /**
     * Finished types, for memoization purposes.
     * This ensures a new ref is not created for the same type.
     */
    private val built = mutableMapOf<String, ObjectNode>()

    /**
     * Names taken in `$defs`. Two classes with the same simple name get distinct definitions.
     */
    private val defNames = mutableSetOf<String>()

    override fun defaultHandler(node: KSNode, data: Node): Node {
        logger.warn("Unhandled node type: ${node::class.simpleName} for $node")

        return data
    }

    /**
     * Visits the [SchemaRoot] class.
     * Nested classes are built from their types instead, see [schemaFor].
     */
    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: Node
    ): Node {
        return objectSchemaFor(classDeclaration, classDeclaration.asStarProjectedType(), isRoot = true)
    }

    /**
     * The schema of a class's properties, or a reference to it if it's already being built further up, i.e. the
     * class contains itself.
     * @param type: The type.
     * @param declaration the KSP declaration.
     * @param isRoot Whether it is the root element or not.
     */
    private fun objectSchemaFor(declaration: KSClassDeclaration, type: KSType, isRoot: Boolean = false): Node {
        // Check if this type is already in progress.
        val key = type.key()
        (inProgress[key] ?: built[key])?.let { return referenceTo(it) }

        // Generic types can nest without ever repeating, e.g. a Box<T> holding a Box<List<T>>.
        if (inProgress.size >= MAX_DEPTH) {
            logger.error("$type is nested more than $MAX_DEPTH objects deep", declaration)

            return PrimitiveNode(PrimitiveType.STRING)
        }

        // New node in progress
        val objectNode = ObjectNode(name = declaration.simpleName.asString(), isRoot = isRoot)
        inProgress[key] = objectNode
        val withoutDefault = declaration.parametersWithoutDefault()
        declaration.getAllProperties()
            // Only properties with a backing field hold data, computed ones aren't serialized.
            .filter { it.hasBackingField }
            // Skip properties marked to be excluded
            .filter { it.shouldNotBeExcluded }
            .forEach {
                objectNode.properties.add(
                    propertySchemaFor(
                        property = it,
                        containingType = type,
                        hasNoDefault = it.simpleName.asString() in withoutDefault
                    )
                )
            }
        inProgress.remove(key)
        built[key] = objectNode

        return objectNode
    }

    /**
     * Builds the schema [Node] of [property] as a member of [containingType].
     * Type parameters are replaced with the type arguments of the containing type.
     * @param hasNoDefault Whether the containing type's constructor takes the property without a default, so a decoder
     * needs it in the JSON. Such properties are required, as are the ones marked with [SchemaRequired].
     */
    private fun propertySchemaFor(
        property: KSPropertyDeclaration,
        containingType: KSType,
        hasNoDefault: Boolean
    ): PropertyNode {
        // Build the schema of the property's type
        val propertyTypeSchema = schemaFor(property, property.asMemberOf(containingType))

        // New node with the obtained schema.
        val propertyNode = PropertyNode(
            name = property.simpleName.asString(),
            declaration = propertyTypeSchema,
            source = property,
            required = hasNoDefault || property.annotation<SchemaRequired>() != null
        )

        // Title and description annotations
        processDescriptiveAnnotations(propertyNode, property)

        // String types and its formatting annotation.
        processStringAnnotations(property, propertyTypeSchema)

        // Integer types and its corresponding annotations.
        processIntegerAnnotations(property, propertyTypeSchema)

        // Array type and its corresponding annotations.
        processArrayAnnotations(property, propertyTypeSchema)

        return propertyNode
    }

    private fun processDescriptiveAnnotations(
        propertyNode: PropertyNode,
        property: KSPropertyDeclaration
    ) {
        propertyNode.title = property.annotation<SchemaTitle>()?.argument("text") as? String
        propertyNode.description = property.annotation<SchemaDescription>()?.argument("text") as? String
    }

    private fun processStringAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    ) {
        property.annotation<SchemaFormat>()?.let { annotation ->
            if (propertyTypeSchema is PrimitiveNode && propertyTypeSchema.type == PrimitiveType.STRING && propertyTypeSchema.enum == null) {
                propertyTypeSchema.format = annotation.enumArgument<SchemaFormat.Format>(StringProperties.FORMAT)?.value
            } else {
                logger.error("@SchemaFormat only applies to String properties", property)
            }
        }
    }

    private fun processIntegerAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    ) {
        val minimum = property.annotation<SchemaMinimum>()?.argument("value") as? Double
        val maximum = property.annotation<SchemaMaximum>()?.argument("value") as? Double
        if (minimum != null && !minimum.isFinite()) {
            logger.error("@SchemaMinimum must be finite, found $minimum", property)
        }
        if (maximum != null && !maximum.isFinite()) {
            logger.error("@SchemaMaximum must be finite, found $maximum", property)
        }
        if (minimum != null || maximum != null) {
            if (propertyTypeSchema is PrimitiveNode && propertyTypeSchema.type in listOf(
                    PrimitiveType.INTEGER,
                    PrimitiveType.NUMBER
                )
            ) {
                // Annotations override the minimum of 0 that unsigned types start with.
                minimum?.let { propertyTypeSchema.minimum = it }
                maximum?.let { propertyTypeSchema.maximum = it }
            } else {
                logger.error("@SchemaMinimum and @SchemaMaximum only apply to number properties", property)
            }
            if (minimum != null && maximum != null && minimum > maximum) {
                logger.error("@SchemaMinimum($minimum) is greater than @SchemaMaximum($maximum)", property)
            }
        }
    }

    private fun processArrayAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    ) {
        val minItems = property.annotation<SchemaMinItems>()?.argument("value") as? Int
        val maxItems = property.annotation<SchemaMaxItems>()?.argument("value") as? Int
        val prefixItems =
            (property.annotation<SchemaPrefixItems>()?.argument("types") as? List<*>)?.filterIsInstance<KSType>()
        if (minItems != null || maxItems != null || prefixItems != null) {
            if (propertyTypeSchema is ArrayNode) {
                propertyTypeSchema.minItems = minItems
                propertyTypeSchema.maxItems = maxItems
                propertyTypeSchema.prefixItems = prefixItems.orEmpty().map { schemaFor(property, it) }
            } else {
                logger.error(
                    "@SchemaMinItems, @SchemaMaxItems and @SchemaPrefixItems only apply to collection properties",
                    property
                )
            }

            if (minItems != null && minItems < 0) {
                logger.error("@SchemaMinItems($minItems) can't be negative", property)
            }
            if (maxItems != null && maxItems < 0) {
                logger.error("@SchemaMaxItems($maxItems) can't be negative", property)
            }
            if (minItems != null && maxItems != null && minItems > maxItems) {
                logger.error("@SchemaMinItems($minItems) is greater than @SchemaMaxItems($maxItems)", property)
            }
        }
    }

    /**
     * The schema of [type]: a primitive, an array of its element type, or an object with its properties.
     * @param symbol Where errors are reported.
     */
    private fun schemaFor(symbol: KSNode, type: KSType): Node {
        val schema = nonNullSchemaFor(symbol, type)
        // A type alias can be nullable itself, or be used as nullable.
        schema.nullable = schema.nullable || type.isMarkedNullable

        return schema
    }

    private fun nonNullSchemaFor(symbol: KSNode, type: KSType): Node {
        val declaration = type.declaration
        val qualifiedName = declaration.qualifiedName?.asString()
        return when {
            // Resolve type aliases
            declaration is KSTypeAlias -> schemaFor(symbol, declaration.type.resolve())

            declaration is KSTypeParameter -> unsupported(
                "The type parameter ${declaration.name.asString()} has no type argument to describe; use a concrete type.",
                symbol
            )

            // Primitive types including arrays
            qualifiedName in knownIntegerTypes -> PrimitiveNode(PrimitiveType.INTEGER)
            qualifiedName in knownUnsignedTypes -> PrimitiveNode(PrimitiveType.INTEGER).apply { minimum = 0.0 }
            qualifiedName in knownNumberTypes -> PrimitiveNode(PrimitiveType.NUMBER)
            qualifiedName in knownBooleanTypes -> PrimitiveNode(PrimitiveType.BOOLEAN)
            qualifiedName in knownStringTypes -> PrimitiveNode(PrimitiveType.STRING)
            qualifiedName in knownPrimitiveArrayTypes -> ArrayNode(
                PrimitiveNode(
                    knownPrimitiveArrayTypes.getValue(
                        qualifiedName!!
                    )
                )
            )

            // Enum support
            declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS ->
                PrimitiveNode(PrimitiveType.STRING, declaration.enumEntries())

            // Array support
            declaration.isArray() -> {
                val elementType = type.arguments.firstOrNull()?.type?.resolve()
                    ?: return unsupported("cannot resolve the element type of $type", symbol)
                ArrayNode(schemaFor(symbol, elementType))
            }

            // Additional Properties support
            // A map is an object whose keys aren't known up front, so its values are described by additionalProperties.
            declaration.isMap() -> {
                // Check type parameters at <0, 1>
                val keyType = type.arguments.getOrNull(0)?.type?.resolve()
                val valueType = type.arguments.getOrNull(1)?.type?.resolve()
                if (keyType == null || valueType == null) {
                    return unsupported("cannot resolve the key and value types of $type", symbol)
                }

                // JSON object keys are always strings.
                if (keyType.declaration.qualifiedName?.asString() != getQualifiedName<String>()) {
                    logger.error("Map keys must be String, found $keyType in $type", symbol)
                }

                MapNode(schemaFor(symbol, valueType))
            }

            qualifiedName == "kotlin.Any" -> unsupported("Any could be anything; use a concrete type", symbol)

            declaration !is KSClassDeclaration -> unsupported("$type isn't a class", symbol)

            // TODO: Describe sealed classes and interfaces with anyOf.
            declaration.classKind == ClassKind.INTERFACE || Modifier.SEALED in declaration.modifiers ||
                    Modifier.ABSTRACT in declaration.modifiers ->
                unsupported("interfaces, sealed and abstract classes like $type aren't supported yet", symbol)

            // Object type, continue with its properties.
            else -> objectSchemaFor(declaration, type.makeNotNullable())
        }
    }

    /**
     * Marks [target] as referenced, naming it in `$defs` unless it's the root (`#`), and points to it.
     */
    private fun referenceTo(target: ObjectNode): RefNode {
        if (!target.referenced) {
            target.referenced = true
            if (!target.isRoot) {
                target.defName = generateUniqueDefName(target.name)
            }
        }

        return RefNode(target)
    }

    /**
     * Reports [reason] as an error, and returns a placeholder so processing continues and
     * reports any other errors too.
     */
    private fun unsupported(reason: String, symbol: KSNode): Node {
        logger.error("No schema for this property: $reason", symbol)

        return PrimitiveNode(PrimitiveType.STRING)
    }

    /**
     * Generates a unique ref name.
     */
    private fun generateUniqueDefName(name: String): String {
        var candidate = name
        var suffix = 2
        while (!defNames.add(candidate)) {
            candidate = "$name${suffix++}"
        }
        return candidate
    }
}
