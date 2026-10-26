package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.vectornorthsystems.jsonschema.SchemaRequired
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.getQualifiedName
import com.vectornorthsystems.jsonschema.processor.node.*


/**
 * A visitor that walks the hierarchy and produces the tree of properties and types using [com.vectornorthsystems.jsonschema.processor.node.Node].
 */
internal class SchemaVisitor(
    val annotationProcessor: AnnotationProcessor,
    val typeClassifier: TypeClassifier,
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
        return objectSchemaFor(
            declaration = classDeclaration,
            type = classDeclaration.asStarProjectedType(),
            isRoot = true
        )
    }

    /**
     * Returns the schema node of a class's properties, or a reference to it if it's an already known type.
     *
     * @param declaration The KSP declaration.
     * @param type: The type.
     * @param isRoot Whether it is the root element or not.
     */
    private fun objectSchemaFor(declaration: KSClassDeclaration, type: KSType, isRoot: Boolean = false): Node {
        // Check if this type is already in progress or otherwise known.
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
        // Obtain the list of properties with default values. Used below.
        val withoutDefault = declaration.parametersWithoutDefault()
        declaration.getAllProperties()
            // Only properties with a backing field hold data, computed ones aren't serialized.
            .filter { it.hasBackingField }
            // Filter properties marked to be excluded.
            .filter { it.shouldNotBeExcluded }
            .forEach {
                // Build the node properties.
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
     *
     * Type parameters are replaced with the type arguments of the containing type.
     * @param property The native property declaration to build.
     * @param containingType The type of the property.
     * @param hasNoDefault Whether the containing type's constructor takes the property without a default, so a decoder
     * needs it in the JSON. Such properties are required, as are the ones marked with [SchemaRequired].
     */
    private fun propertySchemaFor(
        property: KSPropertyDeclaration,
        containingType: KSType,
        hasNoDefault: Boolean
    ): PropertyNode {
        // Build the schema of the property's type first.
        val propertyTypeSchema = schemaFor(type = property.asMemberOf(containingType), symbol = property)

        // New node with the obtained schema.
        val propertyNode = PropertyNode(
            name = property.simpleName.asString(),
            declaration = propertyTypeSchema,
            source = property,
            required = hasNoDefault || property.annotation<SchemaRequired>() != null
        )

        // Title and description annotations: apply to all properties.
        annotationProcessor.processDescriptiveAnnotations(property, propertyNode)

        // Formatting annotations that apply to String properties.
        annotationProcessor.processStringAnnotations(property, propertyTypeSchema)

        //  Annotations that apply to integer types.
        annotationProcessor.processIntegerAnnotations(property, propertyTypeSchema)

        // Annotations that apply to array types.
        annotationProcessor.processArrayAnnotations(property, propertyTypeSchema) {
            schemaFor(type = it, symbol = property)
        }

        return propertyNode
    }

    /**
     * The schema of [type]: a primitive, an array of its element type, or an object with its properties.
     *
     * @param type The type to build the node for
     * @param symbol Where errors are reported.
     *
     * @return A new [Node] of the corresponding type.
     */
    private fun schemaFor(type: KSType, symbol: KSNode): Node {
        val schema = nonNullSchemaFor(type, symbol)
        // A type alias can be nullable itself, or be used as nullable.
        schema.nullable = schema.nullable || type.isMarkedNullable

        return schema
    }

    private fun nonNullSchemaFor(type: KSType, symbol: KSNode): Node =
        when (val shape = typeClassifier.classify(type)) {
            is TypeShape.Alias -> schemaFor(type = shape.target, symbol = symbol)
            is TypeShape.Primitive -> PrimitiveNode(shape.type).apply { minimum = shape.minimum }
            is TypeShape.PrimitiveCollection -> primitiveArraySchemaFor(shape.qualifiedName)
            is TypeShape.EnumOf -> PrimitiveNode(PrimitiveType.STRING, shape.entries)
            is TypeShape.Collection -> ArrayNode(schemaFor(type = shape.element, symbol = symbol))
            is TypeShape.MapOf -> mapSchemaFor(shape, type, symbol)
            is TypeShape.Value -> schemaFor(type = shape.underlying, symbol = symbol)
            is TypeShape.Object -> objectSchemaFor(shape.declaration, type.makeNotNullable())
            is TypeShape.Unsupported -> unsupported(shape.reason, symbol)
        }

    private fun primitiveArraySchemaFor(qualifiedName: String): Node =
        ArrayNode(PrimitiveNode(knownPrimitiveArrayTypes.getValue(qualifiedName)))

    private fun mapSchemaFor(shape: TypeShape.MapOf, type: KSType, symbol: KSNode): Node {
        // JSON object keys are always strings.
        if (shape.key.declaration.qualifiedName?.asString() != getQualifiedName<String>()) {
            logger.error("Map keys must be String, found ${shape.key} in $type", symbol)
        }

        return MapNode(schemaFor(type = shape.value, symbol = symbol))
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
