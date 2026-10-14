package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * Maps are handled as additionalProperties.
 */
@SchemaRoot
data class Additional(
    val labels: Map<String, String> = emptyMap(),
    val series: MutableMap<String, List<Int>> = mutableMapOf(),
    val parts: Map<String, Quantity> = emptyMap(),
)