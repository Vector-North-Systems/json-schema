package com.vectornorthsystems.jsonschema

/**
 * The supported target dialects of JSON schema.
 *
 * Each AI provider accepts its own subset of JSON schema, with its own rules. A [SchemaRoot] class gets one schema per
 * dialect, adapted to those rules. A class that a dialect can't express fails the build; generate only the dialects
 * you use, with the `jsonSchema.targets` KSP argument or [SchemaRoot.targets].
 */
enum class SchemaTarget {
    /**
     * The Google Gemini Structured Output dialect of JSON schema.
     *
     * Primary constructor parameters without a default are required, and so are the properties marked with
     * [SchemaRequired]. The rest are optional, so the AI model may leave them out. Maps are objects whose
     * `additionalProperties` describe their values.
     */
    GEMINI,

    /**
     * The OpenAI Structured Outputs dialect of JSON schema, in strict mode.
     *
     * Every property is required, whether it has a default, so [SchemaRequired] changes nothing. Use a nullable
     * type for a value the AI model may not have. Every object has `additionalProperties` set to `false`, so maps
     * aren't supported and fail the build.
     */
    OPENAI
}
