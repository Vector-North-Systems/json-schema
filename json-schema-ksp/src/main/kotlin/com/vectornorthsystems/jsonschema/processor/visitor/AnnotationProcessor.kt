package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode

/**
 * Processes annotations.
 */
internal interface AnnotationProcessor {
    /**
     * Processes annotations that apply to all property types.
     *
     * @param property The annotated property.
     * @param propertyNode The property node receiving the annotation value.
     */
    fun processDescriptiveAnnotations(
        property: KSPropertyDeclaration,
        propertyNode: PropertyNode
    )

    /**
     * Processes annotations that apply to [String] properties.
     *
     * @param property The annotated property.
     * @param propertyTypeSchema The node of the type of the property receiving the annotation value.
     */
    fun processStringAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    )

    /**
     * Processes annotations that apply to [Integer] properties.
     *
     * @param property The annotated property.
     * @param propertyTypeSchema The node of the type of the property receiving the annotation value.
     */
    fun processIntegerAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    )

    /**
     * Processes annotations that apply to array properties.
     * @param schemaFor Builds the schema of a type named by an annotation.
     * Should be removed when [com.vectornorthsystems.jsonschema.SchemaPrefixItems] is removed.
     *
     * @param property The annotated property.
     * @param propertyTypeSchema The node of the type of the property receiving the annotation value.
     */
    fun processArrayAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node,
        schemaFor: (KSType) -> Node
    )
}