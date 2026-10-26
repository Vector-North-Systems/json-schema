package com.vectornorthsystems

import com.vectornorthsystems.fixtures.*

import kotlin.test.Test

/**
 * The name and visibility of the generated function.
 */
class GeneratedFunctionTest {

    @Test
    fun `functionName names the generated function`() {
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
            actual = pointJsonSchema()
        )
    }

    @Test
    fun `nested classes with the same name generate separate functions`() {
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
            actual = getSearchRequestSchema()
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
            actual = getLoginRequestSchema()
        )
    }
}
