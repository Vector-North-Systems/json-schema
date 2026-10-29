package com.vectornorthsystems.samples

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
}
