package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

@SchemaRoot(name = "PointJsonSchema")
data class Point(
    val x: Int = 0,
    val y: Int = 0,
)

// Nested classes with the same name would both generate RequestSchema, so they need their own names.
class Search {
    @SchemaRoot(name = "SearchRequestSchema")
    data class Request(val query: String = "")
}

class Login {
    @SchemaRoot(name = "LoginRequestSchema")
    data class Request(val user: String = "")
}
