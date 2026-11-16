package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.transformers.BaseTransformerImpl

/**
 * Sets `additionalProperties: false` on every object.
 *
 * It edits the copy `super` returns rather than copying it again: [ObjectNode.copy] doesn't carry the properties, and
 * references already point at that copy.
 */
internal class SetAdditionalPropertiesFalse : BaseTransformerImpl() {
    override fun transformObjectNode(node: ObjectNode): ObjectNode {
        return super.transformObjectNode(node).apply { additionalProperties = false }
    }
}
