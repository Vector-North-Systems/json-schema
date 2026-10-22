package com.vectornorthsystems

import com.vectornorthsystems.fixtures.*

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Checks the schemas generated for the fixtures in `src/main/kotlin`. Each fixture is a [com.vectornorthsystems.jsonschema.SchemaRoot], so building this
 * module runs the processor on it, and a fixture the processor rejects fails the build.
 */
class SchemaTest {

    private fun assertSchema(expected: String, actual: String) {
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
    }

    @Test
    fun `complex is simple`() {
        assertSchema(expected ="""
            {
              "type": "object",
              "properties": {
                "real": {"type": "number"},
                "imaginary": {"type": "number"}
              }
            }
            """, actual = getComplexSchema())
    }

    @Test
    fun `objects, arrays, enums and parameters without a default are required`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "ingredients": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string"},
                      "quantity": {
                        "type": "object",
                        "properties": {"amount": {"type": "number"}, "unit": {"type": "string"}},
                        "required": ["amount"]
                      },
                      "optional": {"type": "boolean"}
                    },
                    "required": ["name", "quantity"]
                  }
                },
                "instructions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {"stepNumber": {"type": "integer"}, "description": {"type": "string"}}
                  }
                },
                "unitSystem": {"type": "string", "enum": ["METRIC", "IMPERIAL"]}
              },
              "required": ["name", "ingredients"]
            }
            """,
            getRecipeSchema()
        )
    }

    @Test
    fun `a root that contains itself is referenced with #`() {
        assertSchema(
            $$"""
            {
              "type": "object",
              "properties": {
                "label": {"type": "string"},
                "children": {"type": "array", "items": {"$ref": "#"}}
              }
            }
            """,
            getTreeNodeSchema()
        )
    }

    @Test
    fun `a nested class that contains itself is defined once in defs`() {
        assertSchema(
            $$"""
            {
              "type": "object",
              "properties": {"head": {"$ref": "#/$defs/Employee"}},
              "$defs": {
                "Employee": {
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"},
                    "reports": {"type": "array", "items": {"$ref": "#/$defs/Employee"}},
                    "manager": {"anyOf": [{"$ref": "#/$defs/Employee"}, {"type": "null"}]}
                  }
                }
              }
            }
            """,
            getOrganizationSchema()
        )
    }

    @Test
    fun `the same nested class should exist only once`() {
        assertSchema(
            $$"""
            {
              "type": "object",
              "properties": {
                "ceo": {"$ref": "#/$defs/Employee"},
                "cto": {"$ref": "#/$defs/Employee"}
              },
              "$defs": {
                "Employee": {
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"},
                    "reports": {"type": "array", "items": {"$ref": "#/$defs/Employee"}},
                    "manager": {"anyOf": [{"$ref": "#/$defs/Employee"}, {"type": "null"}]}
                  }
                }
              },
              "required": [
                "ceo",
                "cto"
              ]
            }
            """,
            getOrgSchema()
        )
    }

    @Test
    fun `a non-recursive class used twice is defined once and referenced twice`() {
        assertSchema(
            $$"""
            {
              "type": "object",
              "properties": {
                "billing": {"$ref": "#/$defs/Address"},
                "shipping": {"$ref": "#/$defs/Address"}
              },
              "$defs": {
                "Address": {
                  "type": "object",
                  "properties": {
                    "street": {"type": "string"},
                    "city": {"type": "string"}
                  }
                }
              },
              "required": ["billing", "shipping"]
            }
            """,
            getCustomerSchema()
        )
    }

    @Test
    fun `more than 32 sibling classes do not hit the depth limit`() {
        val schema = Json.parseToJsonElement(getWideSchema()).jsonObject
        assertEquals(40, schema.getValue("properties").jsonObject.size)
        assertNull(schema[$$"$defs"])
    }

    @Test
    fun `inherited properties are included, computed and delegated ones are not`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {"email": {"type": "string"}, "id": {"type": "string"}}
            }
            """,
            getAccountSchema()
        )
    }

    @Test
    fun `constructor parameters without a default are required, body properties are optional`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {"joined": {"type": "integer"}, "id": {"type": "string"}},
              "required": ["id"]
            }
            """,
            getMemberSchema()
        )
    }

    @Test
    fun `excluded properties are left out`() {
        assertSchema(
            """{"type": "object", "properties": {"user": {"type": "string"}}}""",
            getSessionSchema()
        )
    }

    @Test
    fun `type parameters take the type arguments of each use`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "label": {"type": "object", "properties": {"value": {"type": "string"}}, "required": ["value"]},
                "count": {"type": "object", "properties": {"value": {"type": "integer"}}, "required": ["value"]},
                "nested": {
                  "type": "object",
                  "properties": {
                    "value": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {"value": {"type": "boolean"}},
                        "required": ["value"]
                      }
                    }
                  },
                  "required": ["value"]
                }
              }
            }
            """,
            getShelfSchema()
        )
    }

    @Test
    fun `nullable types allow null, independently of being required`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "bio": {"type": ["string", "null"]},
                "age": {"type": ["integer", "null"]},
                "tags": {"type": ["array", "null"], "items": {"type": ["string", "null"]}},
                "unitSystem": {"type": ["string", "null"], "enum": ["METRIC", "IMPERIAL"]},
                "quantity": {
                  "type": ["object", "null"],
                  "properties": {"amount": {"type": "number"}, "unit": {"type": "string"}},
                  "required": ["amount"]
                },
                "nickname": {"type": ["string", "null"]},
                "website": {"type": ["string", "null"]}
              },
              "required": ["name", "bio", "website"]
            }
            """,
            getProfileSchema()
        )
    }

    @Test
    fun `annotations add their keywords`() {
        assertSchema(
            $$"""
            {
              "type": "object",
              "properties": {
                "name": {"title": "Name", "description": "Costs $5 \"or\" less", "type": "string"},
                "notes": {"type": "string"},
                "startsAt": {"type": "string", "format": "date-time"},
                "rating": {"type": "number", "minimum": 1, "maximum": 4.5},
                "attendees": {"type": "integer", "minimum": 0},
                "speakers": {"type": "array", "items": {"type": "string"}, "minItems": 1, "maxItems": 20},
                "location": {
                  "type": "array",
                  "prefixItems": [{"type": "string"}, {"type": "integer"}],
                  "items": {"type": "string"}
                }
              },
              "required": ["name", "startsAt"]
            }
            """,
            getEventSchema()
        )
    }

    @Test
    fun `chars, unsigned numbers and primitive arrays`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "grade": {"type": "string"},
                "level": {"type": "integer", "minimum": 0, "maximum": 10},
                "samples": {"type": "array", "items": {"type": "integer"}},
                "flags": {"type": "array", "items": {"type": "boolean"}}
              }
            }
            """,
            getMeasurementsSchema()
        )
    }

    @Test
    fun `mutable and read-only collections are arrays`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "array": {"type": "array", "items": {"type": "string"}},
                "list": {"type": "array", "items": {"type": "string"}},
                "mutableList": {"type": "array", "items": {"type": "string"}},
                "set": {"type": "array", "items": {"type": "integer"}},
                "mutableSet": {"type": "array", "items": {"type": "integer"}},
                "collection": {"type": "array", "items": {"type": "boolean"}},
                "mutableCollection": {"type": "array", "items": {"type": "boolean"}},
                "iterable": {"type": "array", "items": {"type": "number"}},
                "mutableIterable": {"type": "array", "items": {"type": "number"}}
              }
            }
            """,
            getContainersSchema()
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
            actual = getAdditionalSchema()
        )
    }

    @Test
    fun `functionName names the generated function`() {
        assertSchema(
            """
            {
              "type": "object",
              "properties": {
                "x": {"type": "integer"},
                "y": {"type": "integer"}
              }
            }
            """,
            actual = pointJsonSchema()
        )
    }

    @Test
    fun `nested classes with the same name generate separate functions`() {
        assertSchema(
            """{"type": "object", "properties": {"query": {"type": "string"}}}""",
            actual = getSearchRequestSchema()
        )
        assertSchema(
            """{"type": "object", "properties": {"user": {"type": "string"}}}""",
            actual = getLoginRequestSchema()
        )
    }
}
