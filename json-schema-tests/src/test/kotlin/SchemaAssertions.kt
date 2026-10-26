package com.vectornorthsystems

import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

/**
 * Compares two schemas as JSON, ignoring formatting and the order of keys.
 */
internal fun assertSchema(expected: String, actual: String) {
    assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
}
