package com.vectornorthsystems.jsonschema.processor.transformers

import com.vectornorthsystems.jsonschema.processor.node.*
import java.util.IdentityHashMap

/**
 * The base implementation.
 * This walks the tree top-down producing a copy, so the original tree is never changed. A subclass overrides only the
 * node kinds it changes, and edits the copy that `super` returns.
 *
 * An instance transforms one tree: it remembers the objects it copied, to point references at the copies.
 */
internal open class BaseTransformerImpl : Transformer {

    /**
     * The copy of each object, by the original. A [RefNode] points back at an object further up the tree or a sibling, which is
     * already copied when the reference is reached.
     */
    private val copies = IdentityHashMap<ObjectNode, ObjectNode>()

    override fun transform(root: Node): Node {
        return transformNode(root)
    }

    /**
     * Transforms any node, dispatching to the hook for its kind.
     */
    protected open fun transformNode(node: Node): Node {
        return when (node) {
            is ObjectNode -> transformObjectNode(node)
            is ArrayNode -> transformArrayNode(node)
            is MapNode -> transformMapNode(node)
            is PrimitiveNode -> transformPrimitiveNode(node)
            is PropertyNode -> transformProperty(node)
            is RefNode -> transformRefNode(node)
        }
    }

    /**
     * Transforms a [PropertyNode] node.
     */
    override fun transformProperty(node: PropertyNode): PropertyNode {
        return PropertyNode(
            name = node.name,
            declaration = transformNode(node.declaration),
            source = node.source,
            required = node.required,
        ).copyCommon(node)
    }

    /**
     * Transforms a [PrimitiveNode] node.
     */
    override fun transformPrimitiveNode(node: PrimitiveNode): PrimitiveNode {
        return PrimitiveNode(
            type = node.type,
            enum = node.enum,
        ).apply {
            format = node.format
            minimum = node.minimum
            maximum = node.maximum
        }.copyCommon(node)
    }

    /**
     * Transforms a [ObjectNode] node.
     */
    override fun transformObjectNode(node: ObjectNode): ObjectNode {
        val copy = ObjectNode(
            name = node.name,
            isRoot = node.isRoot
        ).apply {
            referenced = node.referenced
            defName = node.defName
        }.copyCommon(node)

        // Register before the properties, which may reference this object.
        copies[node] = copy
        node.properties.mapTo(copy.properties) { property -> transformProperty(property) }

        return copy
    }

    /**
     * Transforms a [RefNode] node.
     */
    override fun transformRefNode(node: RefNode): RefNode {
        return RefNode(copies.getValue(node.target)).copyCommon(node)
    }

    /**
     * Transforms a [ArrayNode] node.
     */
    override fun transformArrayNode(node: ArrayNode): ArrayNode {
        return ArrayNode(transformNode(node.declaration)).apply {
            prefixItems = node.prefixItems.map { transformNode(it) }
            minItems = node.minItems
            maxItems = node.maxItems
        }.copyCommon(node)
    }

    /**
     * Transforms a [MapNode] node.
     */
    override fun transformMapNode(node: MapNode): MapNode {
        return MapNode(transformNode(node.declaration)).copyCommon(node)
    }

    /**
     * Copies the fields every [Node] has from [original].
     */
    private fun <T : Node> T.copyCommon(original: Node): T = apply {
        title = original.title
        description = original.description
        nullable = original.nullable
    }
}
