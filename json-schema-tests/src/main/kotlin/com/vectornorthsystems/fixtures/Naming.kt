package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

@SchemaRoot(functionName = "pointJsonSchema")
data class Point(
    val x: Int = 0,
    val y: Int = 0,
)

// Nested classes with the same name would both generate getRequestSchema(), so they need their own names.
class Search {
    @SchemaRoot(functionName = "getSearchRequestSchema")
    data class Request(val query: String = "")
}

class Login {
    @SchemaRoot(functionName = "getLoginRequestSchema")
    data class Request(val user: String = "")
}
