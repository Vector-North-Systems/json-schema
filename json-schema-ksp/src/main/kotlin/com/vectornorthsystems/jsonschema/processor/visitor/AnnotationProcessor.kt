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
     * Processes annotations that apply to all nodes.
     */
    fun processDescriptiveAnnotations(
        propertyNode: PropertyNode,
        property: KSPropertyDeclaration
    )

    /**
     * Processes annotations that apply to [String] properties.
     */
    fun processStringAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    )

    /**
     * Processes annotations that apply to [Integer] properties.
     */
    fun processIntegerAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node
    )

    /**
     * Processes annotations that apply to array properties.
     * @param schemaFor Builds the schema of a type named by an annotation.
     * Should be removed when [com.vectornorthsystems.jsonschema.SchemaPrefixItems] is removed.
     */
    fun processArrayAnnotations(
        property: KSPropertyDeclaration,
        propertyTypeSchema: Node,
        schemaFor: (KSType) -> Node
    )
}