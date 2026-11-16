package com.vectornorthsystems.dialect.openai

import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget

enum class SpectralType {
    O, B, A, F, G, K, M, L, T
}

/**
 * A root that specifies targets, overrides the `.jsonSchema.targets` option.
 */
@SchemaRoot(targets = [SchemaTarget.OPENAI, SchemaTarget.GEMINI])
data class Star(
    val starClass: SpectralType,
    val colorDescription: String,
)