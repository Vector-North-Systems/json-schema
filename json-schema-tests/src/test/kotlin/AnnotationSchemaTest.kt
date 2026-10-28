package com.vectornorthsystems

import com.vectornorthsystems.fixtures.*

import kotlin.test.Test

/**
 * The keywords the schema annotations add.
 */
class AnnotationSchemaTest {

    @Test
    fun `annotations add their keywords`() {
        assertSchema(
            $$"""
            {
              "type": "object",
              "properties": {
                "name": {
                  "title": "Name",
                  "description": "Costs $5 \"or\" less",
                  "type": "string"
                },
                "notes": {
                  "type": "string"
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
                "startsAt"
              ]
            }
            """,
            getEventSchema()
        )
    }
}
