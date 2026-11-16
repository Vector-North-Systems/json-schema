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
internal abstract class BaseTransformerImpl : Transformer {

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
        return node.copy(
            declaration = transformNode(node.declaration)
        )
    }

    /**
     * Transforms a [PrimitiveNode] node.
     */
    override fun transformPrimitiveNode(node: PrimitiveNode): PrimitiveNode {
        return node.copy()
    }

    /**
     * Transforms a [ObjectNode] node.
     */
    override fun transformObjectNode(node: ObjectNode): ObjectNode {
        val copy = node.copy()

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
            minItems = node.minItems
            maxItems = node.maxItems
        }.copyCommon(node)
    }

    /**
     * Transforms a [MapNode] node.
     */
    override fun transformMapNode(node: MapNode): MapNode {
        return MapNode(transformNode(node.declaration), node.source).copyCommon(node)
    }
}

