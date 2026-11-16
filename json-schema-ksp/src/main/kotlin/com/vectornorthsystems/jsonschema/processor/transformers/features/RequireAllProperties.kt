package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import com.vectornorthsystems.jsonschema.processor.transformers.BaseTransformerImpl
import com.vectornorthsystems.jsonschema.processor.transformers.copy

/**
 * Marks every property required, at every depth.
 */
internal class RequireAllProperties : BaseTransformerImpl() {
    override fun transformProperty(node: PropertyNode): PropertyNode {
        return super.transformProperty(node).copy(required = true)
    }
}
