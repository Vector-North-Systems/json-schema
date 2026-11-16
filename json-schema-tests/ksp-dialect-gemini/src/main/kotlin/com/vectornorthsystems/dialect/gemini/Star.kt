package com.vectornorthsystems.dialect.gemini

import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget

enum class SpectralType {
    O, B, A, F, G, K, M, L, T
}

@SchemaRoot(targets = [SchemaTarget.GEMINI, SchemaTarget.OPENAI])
data class Star(
    val starClass: SpectralType,
    val colorDescription: String,
)