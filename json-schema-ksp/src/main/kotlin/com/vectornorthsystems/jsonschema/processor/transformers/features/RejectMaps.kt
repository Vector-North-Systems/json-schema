package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.processing.KSPLogger
import com.vectornorthsystems.jsonschema.processor.node.MapNode
import com.vectornorthsystems.jsonschema.processor.transformers.BaseTransformerImpl

/**
 * Breaks the build if a map is found in the tree, for dialects where it's not supported.
 */
internal class RejectMaps(val dialectName: String, val logger: KSPLogger) : BaseTransformerImpl() {
    override fun transformMapNode(node: MapNode): MapNode {
        logger.error(
            "Map isn't supported by the $dialectName dialect, which requires every object to have " +
                    "fixed properties. Use a data class if the keys are known, or a List of key-value entries.",
            node.source
        )

        return super.transformMapNode(node)
    }
}