package com.vectornorthsystems.fixtures.readme

import com.vectornorthsystems.assertSchema
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Checks the examples in the README and the docs, so the schemas they show are the ones generated. Keep them in sync.
 */
class ReadmeTest {

    @Test
    fun `complex example`() {
        assertEquals(
            """{"type":"object","properties":{"real":{"type":"number"},"imaginary":{"type":"number"}},"required":["real","imaginary"]}""",
            ComplexSchema.GEMINI
        )
    }

    @Test
    fun `complex example for OpenAI`() {
        assertEquals(
            """{"type":"object","properties":{"real":{"type":"number"},"imaginary":{"type":"number"}},"additionalProperties":false,"required":["real","imaginary"]}""",
            ComplexSchema.OPENAI
        )
    }

    @Test
    fun `recipe example for OpenAI`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "instructions": {"type": "string"},
                    "ingredients": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "name": {"type": "string"}
                        },
                        "additionalProperties": false,
                        "required": ["name"]
                      }
                    },
                    "isColdDish": {"type": ["boolean", "null"]}
                  },
                  "additionalProperties": false,
                  "required": ["instructions", "ingredients", "isColdDish"]
                }
            """.trimIndent(),
            RecipeSchema.OPENAI
        )
    }

    @Test
    fun `recipe example`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "instructions": {
                      "type": "string"
                    },
                    "ingredients": {
                      "type": "array",
                      "items": {
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
                    },
                    "isColdDish": {
                      "type": [
                        "boolean",
                        "null"
                      ]
                    }
                  },
                  "required": [
                    "instructions",
                    "ingredients"
                  ]
                }
            """.trimIndent(),
            RecipeSchema.GEMINI
        )
    }

    @Test
    fun `event example`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "title": "Name",
                      "description": "The name of the event, as advertised",
                      "type": "string"
                    },
                    "notes": {
                      "type": [
                        "string",
                        "null"
                      ]
                    },
                    "startsAt": {
                      "type": "string",
                      "format": "date-time"
                    },
                    "rating": {
                      "type": "number",
                      "minimum": 1,
                      "maximum": 4.5
                    },
                    "attendees": {
                      "type": "integer",
                      "minimum": 0
                    },
                    "speakers": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      },
                      "minItems": 1,
                      "maxItems": 20
                    }
                  },
                  "required": [
                    "name",
                    "startsAt",
                    "rating",
                    "speakers"
                  ]
                }
            """.trimIndent(),
            EventSchema.GEMINI
        )
    }

    @Test
    fun `event example for OpenAI`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "title": "Name",
                      "description": "The name of the event, as advertised",
                      "type": "string"
                    },
                    "notes": {"type": ["string", "null"]},
                    "startsAt": {"type": "string", "format": "date-time"},
                    "rating": {"type": "number", "minimum": 1, "maximum": 4.5},
                    "attendees": {"type": "integer", "minimum": 0},
                    "speakers": {
                      "type": "array",
                      "items": {"type": "string"},
                      "minItems": 1,
                      "maxItems": 20
                    }
                  },
                  "additionalProperties": false,
                  "required": ["name", "notes", "startsAt", "rating", "attendees", "speakers"]
                }
            """.trimIndent(),
            EventSchema.OPENAI
        )
    }
}
