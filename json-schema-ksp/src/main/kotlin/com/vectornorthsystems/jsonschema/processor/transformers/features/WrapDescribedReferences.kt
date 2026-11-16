package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import com.vectornorthsystems.jsonschema.processor.transformers.BaseTransformerImpl

/**
 * Wraps the references that have a title or a description in `anyOf`, for the dialects that reject other keywords next
 * to a `$ref`:
 *
 * ```json
 * {"description": "Where the parcel is delivered", "anyOf": [{"$ref": "#/$defs/Address"}]}
 * ```
 *
 * instead of:
 *
 * ```json
 * {"description": "Where the parcel is delivered", "$ref": "#/$defs/Address"}
 * ```
 *
 * The definition stays shared, and the property keeps its own title and description.
 *
 * It marks every property, and the writer wraps the ones that turn out to be a described reference: it's the writer
 * that knows whether a type is written in place or as a reference.
 */
internal class WrapDescribedReferences : BaseTransformerImpl() {
    override fun transformProperty(node: PropertyNode): PropertyNode {
        return super.transformProperty(node).apply { wrapDescribedReference = true }
    }
}
