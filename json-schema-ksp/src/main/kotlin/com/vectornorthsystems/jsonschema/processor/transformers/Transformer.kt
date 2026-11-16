package com.vectornorthsystems.jsonschema.processor.transformers

import com.vectornorthsystems.jsonschema.processor.node.*

/**
 * Transforms a [com.vectornorthsystems.jsonschema.processor.node.Node] tree for a particular JSON schema flavor.
 * Produces a copy of the input tree.
 */
internal interface Transformer {

    /**
     * Transforms the tree starting at [root], returning the transformed copy.
     */
    fun transform(root: Node): Node

    /**
     * Transforms a [PropertyNode] node.
     */
    fun transformProperty(node: PropertyNode): PropertyNode

    /**
     * Transforms a [PrimitiveNode] node.
     */
    fun transformPrimitiveNode(node: PrimitiveNode): PrimitiveNode

    /**
     * Transforms a [ObjectNode] node.
     */
    fun transformObjectNode(node: ObjectNode): ObjectNode

    /**
     * Transforms a [RefNode] node.
     */
    fun transformRefNode(node: RefNode): RefNode

    /**
     * Transforms a [ArrayNode] node.
     */
    fun transformArrayNode(node: ArrayNode): ArrayNode

    /**
     * Transforms a [MapNode] node.
     */
    fun transformMapNode(node: MapNode): MapNode
}
