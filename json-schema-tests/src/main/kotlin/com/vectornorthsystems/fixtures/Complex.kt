package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

@SchemaRoot
data class Complex(
    val real: Double = 0.0,
    val imaginary: Double = 0.0,
)