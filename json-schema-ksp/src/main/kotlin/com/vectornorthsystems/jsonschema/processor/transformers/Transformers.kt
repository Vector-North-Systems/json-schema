package com.vectornorthsystems.jsonschema.processor.transformers

import com.vectornorthsystems.jsonschema.SchemaTarget

/**
 * The transformer that lowers the tree for this dialect.
 *
 * A new dialect needs a branch here: the `when` has no `else`, so the compiler points at it when [SchemaTarget] grows.
 * Each call returns a new instance, since a transformer remembers the objects it copied and so transforms one tree.
 */
internal fun SchemaTarget.transformer(): Transformer = when (this) {
    SchemaTarget.GEMINI -> GeminiTransformer()
}
