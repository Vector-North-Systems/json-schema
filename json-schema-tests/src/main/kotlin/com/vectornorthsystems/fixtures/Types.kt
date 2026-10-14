package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaMaximum
import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * Kotlin types beyond the common primitives.
 * Some of questionable usefulness, yet straightforward to support.
 */
@SchemaRoot
data class Measurements(
    val grade: Char = 'A',
    @SchemaMaximum(10.0)
    val level: UInt = 0u,
    val samples: IntArray = intArrayOf(),
    val flags: BooleanArray = booleanArrayOf(),
)
