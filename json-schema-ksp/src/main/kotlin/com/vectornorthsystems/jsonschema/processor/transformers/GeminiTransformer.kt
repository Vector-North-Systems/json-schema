package com.vectornorthsystems.jsonschema.processor.transformers

import com.vectornorthsystems.jsonschema.SchemaTarget

/**
 * The schemas Gemini accepts. The visitor's tree already fits them, so it's copied as is.
 */
internal class GeminiTransformer : BaseTransformerImpl() {
    override val target: SchemaTarget = SchemaTarget.GEMINI
}
