package com.vectornorthsystems

import com.vectornorthsystems.fixtures.*

import kotlin.test.Test

/**
 * How Kotlin types map to JSON schema types.
 */
class TypeSchemaTest {

    @Test
    fun `nullable enums list null among their values, wherever they appear`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "system": {
                      "type": ["string", "null"],
                      "enum": ["METRIC", "IMPERIAL", null]
                    },
                    "fallback": {
                      "type": "string",
                      "enum": ["METRIC", "IMPERIAL"]
                    },
                    "history": {
                      "type": "array",
                      "items": {
                        "type": ["string", "null"],
                        "enum": ["METRIC", "IMPERIAL", null]
                      }
                    },
                    "bySite": {
                      "type": "object",
                      "additionalProperties": {
                        "type": ["string", "null"],
                        "enum": ["METRIC", "IMPERIAL", null]
                      }
                    },
                    "preferred": {
                      "type": ["array", "null"],
                      "items": {
                        "type": "string",
                        "enum": ["METRIC", "IMPERIAL"]
                      }
                    }
                  },
                  "required": [
                    "system",
                    "fallback",
                    "history",
                    "bySite"
                  ]
                }
            """.trimIndent(),
            UnitPreferencesSchema.GEMINI
        )
    }

    @Test
    fun `nullable types allow null, independently of being required`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string"
                    },
                    "bio": {
                      "type": [
                        "string",
                        "null"
                      ]
                    },
                    "age": {
                      "type": [
                        "integer",
                        "null"
                      ]
                    },
                    "tags": {
                      "type": [
                        "array",
                        "null"
                      ],
                      "items": {
                        "type": [
                          "string",
                          "null"
                        ]
                      }
                    },
                    "unitSystem": {
                      "type": [
                        "string",
                        "null"
                      ],
                      "enum": [
                        "METRIC",
                        "IMPERIAL",
                        null
                      ]
                    },
                    "quantity": {
                      "type": [
                        "object",
                        "null"
                      ],
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
                    "nickname": {
                      "type": [
                        "string",
                        "null"
                      ]
                    },
                    "website": {
                      "type": [
                        "string",
                        "null"
                      ]
                    }
                  },
                  "required": [
                    "name",
                    "bio",
                    "website"
                  ]
                }
            """.trimIndent(),
            ProfileSchema.GEMINI
        )
    }

    @Test
    fun `chars, unsigned numbers and primitive arrays`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "grade": {
                      "type": "string"
                    },
                    "level": {
                      "type": "integer",
                      "minimum": 0,
                      "maximum": 10
                    },
                    "samples": {
                      "type": "array",
                      "items": {
                        "type": "integer"
                      }
                    },
                    "flags": {
                      "type": "array",
                      "items": {
                        "type": "boolean"
                      }
                    }
                  }
                }
            """.trimIndent(),
            MeasurementsSchema.GEMINI
        )
    }

    @Test
    fun `value classes are described by the value they wrap`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "id": {
                      "type": "string"
                    },
                    "previous": {
                      "type": [
                        "string",
                        "null"
                      ]
                    },
                    "ids": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "count": {
                      "type": "integer"
                    }
                  },
                  "required": [
                    "id"
                  ]
                }
            """.trimIndent(),
            SubscriberSchema.GEMINI
        )
    }

    @Test
    fun `value classes wrapping a value class or an object are unwrapped`() {
        assertSchema(
            $$"""
                {
                  "type": "object",
                  "properties": {
                    "handle": {
                      "type": "string"
                    },
                    "home": {
                      "$ref": "#/$defs/Residence"
                    },
                    "previous": {
                      "anyOf": [
                        {
                          "$ref": "#/$defs/Residence"
                        },
                        {
                          "type": "null"
                        }
                      ]
                    }
                  },
                  "required": [
                    "handle",
                    "home"
                  ],
                  "$defs": {
                    "Residence": {
                      "type": "object",
                      "properties": {
                        "city": {
                          "type": "string"
                        },
                        "zip": {
                          "type": "string"
                        }
                      },
                      "required": [
                        "city",
                        "zip"
                      ]
                    }
                  }
                }
            """.trimIndent(),
            ResidentSchema.GEMINI
        )
    }

    @Test
    fun `mutable and read-only collections are arrays`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "array": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "list": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "mutableList": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    },
                    "set": {
                      "type": "array",
                      "items": {
                        "type": "integer"
                      }
                    },
                    "mutableSet": {
                      "type": "array",
                      "items": {
                        "type": "integer"
                      }
                    },
                    "collection": {
                      "type": "array",
                      "items": {
                        "type": "boolean"
                      }
                    },
                    "mutableCollection": {
                      "type": "array",
                      "items": {
                        "type": "boolean"
                      }
                    },
                    "iterable": {
                      "type": "array",
                      "items": {
                        "type": "number"
                      }
                    },
                    "mutableIterable": {
                      "type": "array",
                      "items": {
                        "type": "number"
                      }
                    }
                  }
                }
            """.trimIndent(),
            ContainersSchema.GEMINI
        )
    }

    @Test
    fun `maps express the possibility of additional properties`() {
        assertSchema(
            expected = """
                {
                  "type": "object",
                  "properties": {
                    "labels": {
                      "type": "object",
                      "additionalProperties": {
                        "type": "string"
                      }
                    },
                    "series": {
                      "type": "object",
                      "additionalProperties": {
                        "type": "array",
                        "items": {
                          "type": "integer"
                        }
                      }
                    },
                    "parts": {
                      "type": "object",
                      "additionalProperties": {
                        "type": "object",
                        "properties": {
                          "amount": {
                            "type": "number"
                          },
                          "unit": {
                            "type": "string"
                          }
                        },
                        "required": ["amount"]
                      }
                    }
                  }
                }
                """,
            actual = AdditionalSchema.GEMINI
        )
    }
}
