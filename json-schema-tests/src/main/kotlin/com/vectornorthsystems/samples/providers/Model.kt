package com.vectornorthsystems.samples.providers

import com.vectornorthsystems.jsonschema.SchemaTarget

/**
 * A model that answers in JSON matching a schema.
 *
 * Closing it releases the connection it holds.
 */
interface Model : AutoCloseable {
    /**
     * Sends [input] with [schema] as the response format, and returns the model's answer, i.e.: the JSON that should match
     * the schema.
     */
    suspend fun generate(input: String, schema: String): String

    /**
     * The target dialect this model supports and expects.
     */
    val target: SchemaTarget
}
