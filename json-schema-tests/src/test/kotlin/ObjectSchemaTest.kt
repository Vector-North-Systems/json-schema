package com.vectornorthsystems

import com.vectornorthsystems.fixtures.*

import kotlin.test.Test

/**
 * Object structure and required properties
 */
class ObjectSchemaTest {

    @Test
    fun `complex is simple`() {
        assertSchema(
            expected = """
            {
              "type": "object",
              "properties": {
                "real": {
                  "type": "number"
                },
                "imaginary": {
                  "type": "number"
                }
              }
            }
            """, actual = getComplexSchema()
        )
    }

    @Test
    fun `objects, arrays, enums and parameters without a default are required`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "ingredients": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      },
                      "quantity": {
                        "type": "object",
                        "properties": {
                          "amount": {
                            "type": "number"
                          },
                          "unit": {
                            "type": "string"
                          }
                        },
                        "required": [
                          "amount"
                        ]
                      },
                      "optional": {
                        "type": "boolean"
                      }
                    },
                    "required": [
                      "name",
                      "quantity"
                    ]
                  }
                },
                "instructions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "stepNumber": {
                        "type": "integer"
                      },
                      "description": {
                        "type": "string"
                      }
                    }
                  }
                },
                "unitSystem": {
                  "type": "string",
                  "enum": [
                    "METRIC",
                    "IMPERIAL"
                  ]
                }
              },
              "required": [
                "name",
                "ingredients"
              ]
            }
            """,
            getRecipeSchema()
        )
    }

    @Test
    fun `inherited properties are included, computed and delegated ones are not`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "email": {
                      "type": "string"
                    },
                    "id": {
                      "type": "string"
                    }
                  }
                }
            """.trimIndent(),
            getAccountSchema()
        )
    }

    @Test
    fun `constructor parameters without a default are required, body properties are optional`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "joined": {
                      "type": "integer"
                    },
                    "id": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "id"
                  ]
                }
            """.trimIndent(),
            getMemberSchema()
        )
    }

    @Test
    fun `body properties must be initialized, so they are optional`() {
        assertSchema(
            expected = """
                {
                  "type": "object",
                  "properties": {
                    "length": {
                      "type": "integer"
                    },
                    "mass": {
                      "type": "number"
                    }
                  },
                  "required": [
                    "length"
                  ]
                }
                """.trimIndent(),
            actual = getBodyPropertiesSchema()
        )
    }

    @Test
    fun `excluded properties are left out`() {
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
            getSessionSchema()
        )
    }
}
