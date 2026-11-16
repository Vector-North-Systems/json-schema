package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget

/**
 * Maps are handled as additionalProperties.
 * Gemini: Supported
 * OpenAi: Not Supported
 */
@SchemaRoot(targets = [SchemaTarget.GEMINI])
data class Additional(
    val labels: Map<String, String> = emptyMap(),
    val series: MutableMap<String, List<Int>> = mutableMapOf(),
    val parts: Map<String, Quantity> = emptyMap(),
)