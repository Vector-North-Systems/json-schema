package com.vectornorthsystems.jsonschema.processor.transformers

import com.vectornorthsystems.jsonschema.processor.node.Node

/**
 * A transformer composed of multiple transforming features.
 *
 * It copies the tree, then passes it through each feature in order, each one transforming the previous one's output.
 *
 * @property features The transforming features, applied in this order.
 */
internal class CompositeTransformer(vararg val features: Transformer) :
    BaseTransformerImpl() {
    override fun transform(root: Node): Node {
        return features.fold(super.transform(root)) { current, step -> step.transform(current) }
    }
}