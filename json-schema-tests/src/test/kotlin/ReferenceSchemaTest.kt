package com.vectornorthsystems

import com.vectornorthsystems.fixtures.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Recursion, shared classes and generics, which are defined once in `$defs`.
 */
class ReferenceSchemaTest {

    @Test
    fun `a root that contains itself is referenced with #`() {
        assertSchema(
            $$"""
                {
                  "type": "object",
                  "properties": {
                    "label": {
                      "type": "string"
                    },
                    "children": {
                      "type": "array",
                      "items": {
                        "$ref": "#"
                      }
                    }
                  }
                }
            """.trimIndent(),
            getTreeNodeSchema()
        )
    }

    @Test
    fun `a nested class that contains itself is defined once in defs`() {
        assertSchema(
            $$"""
                {
                  "type": "object",
                  "properties": {
                    "head": {
                      "$ref": "#/$defs/Employee"
                    }
                  },
                  "$defs": {
                    "Employee": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string"
                        },
                        "reports": {
                          "type": "array",
                          "items": {
                            "$ref": "#/$defs/Employee"
                          }
                        },
                        "manager": {
                          "anyOf": [
                            {
                              "$ref": "#/$defs/Employee"
                            },
                            {
                              "type": "null"
                            }
                          ]
                        }
                      }
                    }
                  }
                }
            """.trimIndent(),
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
                    "ceo": {
                      "$ref": "#/$defs/Employee"
                    },
                    "cto": {
                      "$ref": "#/$defs/Employee"
                    }
                  },
                  "$defs": {
                    "Employee": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string"
                        },
                        "reports": {
                          "type": "array",
                          "items": {
                            "$ref": "#/$defs/Employee"
                          }
                        },
                        "manager": {
                          "anyOf": [
                            {
                              "$ref": "#/$defs/Employee"
                            },
                            {
                              "type": "null"
                            }
                          ]
                        }
                      }
                    }
                  },
                  "required": [
                    "ceo",
                    "cto"
                  ]
                }
            """.trimIndent(),
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
                    "billing": {
                      "$ref": "#/$defs/Address"
                    },
                    "shipping": {
                      "$ref": "#/$defs/Address"
                    }
                  },
                  "$defs": {
                    "Address": {
                      "type": "object",
                      "properties": {
                        "street": {
                          "type": "string"
                        },
                        "city": {
                          "type": "string"
                        }
                      }
                    }
                  },
                  "required": [
                    "billing",
                    "shipping"
                  ]
                }
            """.trimIndent(),
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
    fun `type parameters take the type arguments of each use`() {
        assertSchema(
            """
                {
                  "type": "object",
                  "properties": {
                    "label": {
                      "type": "object",
                      "properties": {
                        "value": {
                          "type": "string"
                        }
                      },
                      "required": [
                        "value"
                      ]
                    },
                    "count": {
                      "type": "object",
                      "properties": {
                        "value": {
                          "type": "integer"
                        }
                      },
                      "required": [
                        "value"
                      ]
                    },
                    "nested": {
                      "type": "object",
                      "properties": {
                        "value": {
                          "type": "array",
                          "items": {
                            "type": "object",
                            "properties": {
                              "value": {
                                "type": "boolean"
                              }
                            },
                            "required": [
                              "value"
                            ]
                          }
                        }
                      },
                      "required": [
                        "value"
                      ]
                    }
                  }
                }
            """.trimIndent(),
            getShelfSchema()
        )
    }
}
