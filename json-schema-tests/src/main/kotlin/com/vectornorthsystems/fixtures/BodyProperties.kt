package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

@SchemaRoot
class BodyProperties(
    val length: Int,
) {
    val mass: Double = 100.0
}