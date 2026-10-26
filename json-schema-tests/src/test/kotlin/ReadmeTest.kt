package com.vectornorthsystems

import com.vectornorthsystems.fixtures.readme.getComplexSchema
import com.vectornorthsystems.fixtures.readme.getEventSchema
import com.vectornorthsystems.fixtures.readme.getRecipeSchema
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Checks the examples in the README, so the schemas it shows are the ones generated. Keep both in sync.
 */
class ReadmeTest {

    private fun assertSchema(expected: String, actual: String) {
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
    }

    @Test
    fun `complex example`() {
        assertEquals(
            """{"type":"object","properties":{"real":{"type":"number"},"imaginary":{"type":"number"}},"required":["real","imaginary"]}""",
            getComplexSchema()
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
            getRecipeSchema()
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
            getEventSchema()
        )
    }
}
