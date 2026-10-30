package com.vectornorthsystems.fixtures

import com.vectornorthsystems.assertSchema
import com.vectornorthsystems.jsonschema.SchemaTarget
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The name, visibility and accessors of the generated object.
 */
class GeneratedObjectTest {

    @Test
    fun `name names the generated object`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "x": {
                  "type": "integer"
                },
                "y": {
                  "type": "integer"
                }
              }
            }
            """,
            actual = PointJsonSchema.GEMINI
        )
    }

    @Test
    fun `nested classes with the same name generate separate objects`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string"
                    }
                  }
                }
            """.trimIndent(),
            actual = SearchRequestSchema.GEMINI
        )
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "user": {
                      "type": "string"
                    }
                  }
                }
            """.trimIndent(),
            actual = LoginRequestSchema.GEMINI
        )
    }

    @Test
    fun `internal and private classes get an internal object`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "token": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "token"
                  ]
                }
            """.trimIndent(),
            SecretSchema.GEMINI
        )
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "key": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "key"
                  ]
                }
            """.trimIndent(),
            HiddenSchema.GEMINI
        )
    }

    // TODO: When SchemaTarget has a second dialect, test a class that leaves one out, with
    //  @SchemaRoot(targets = [...]): getOrNull gives null for it, and get throws an IllegalArgumentException naming the
    //  object, the dialects it has, and where to add the missing one. With GEMINI alone, every class has every dialect,
    //  so the `else` branch is only covered by SchemaObjectGeneratorImplTest.
    @Test
    fun `get and getOrNull give the constant of a generated dialect`() {
        // Given a class with every dialect generated...
        // When picking the dialect at runtime...
        // We expect the same schema as its constant:
        assertEquals(PointJsonSchema.GEMINI, PointJsonSchema[SchemaTarget.GEMINI])
        assertEquals(PointJsonSchema.GEMINI, PointJsonSchema.getOrNull(SchemaTarget.GEMINI))
    }

    @Test
    fun `a class can set its own dialects`() {
        // Given a class with @SchemaRoot(targets = [SchemaTarget.GEMINI])...
        // When reading its schema...
        // We expect the dialect it asked for:
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "name"
                  ]
                }
            """.trimIndent(),
            PlanetSchema.GEMINI
        )
        assertEquals(PlanetSchema.GEMINI, PlanetSchema.getOrNull(SchemaTarget.GEMINI))
    }
}
