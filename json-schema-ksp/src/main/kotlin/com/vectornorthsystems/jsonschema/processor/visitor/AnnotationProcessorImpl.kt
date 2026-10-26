package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.vectornorthsystems.jsonschema.*
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.ArrayNode
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.node.PrimitiveNode
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import com.vectornorthsystems.jsonschema.processor.writer.StringProperties


internal class AnnotationProcessorImpl(
    val logger: KSPLogger,
) : AnnotationProcessor {

    override fun processDescriptiveAnnotations(
        property: KSPropertyDeclaration,
        propertyNode: PropertyNode
    ) {
        propertyNode.title = property.annotation<SchemaTitle>()?.argument("text") as? String
        propertyNode.description = property.annotation<SchemaDescription>()?.argument("text") as? String
    }

    override fun processStringAnnotations(
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

    override fun processIntegerAnnotations(
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

    override fun processArrayAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node,
        schemaFor: (KSType) -> Node
    ) {
        val minItems = property.annotation<SchemaMinItems>()?.argument("value") as? Int
        val maxItems = property.annotation<SchemaMaxItems>()?.argument("value") as? Int
        val prefixItems =
            (property.annotation<SchemaPrefixItems>()?.argument("types") as? List<*>)?.filterIsInstance<KSType>()
        if (minItems != null || maxItems != null || prefixItems != null) {
            if (propertyTypeSchema is ArrayNode) {
                propertyTypeSchema.minItems = minItems
                propertyTypeSchema.maxItems = maxItems
                propertyTypeSchema.prefixItems = prefixItems.orEmpty().map(schemaFor)
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
}

