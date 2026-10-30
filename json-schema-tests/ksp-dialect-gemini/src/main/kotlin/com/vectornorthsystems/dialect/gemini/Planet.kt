package com.vectornorthsystems.dialect.gemini

import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * A root without its own targets, so it gets the dialects of the `jsonSchema.targets` option.
 */
@SchemaRoot
data class Planet(val name: String)
