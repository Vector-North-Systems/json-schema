package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

data class Box<T>(val value: T)

/**
 * Type parameters take the type arguments of each use.
 */
@SchemaRoot
data class Shelf(
    val label: Box<String> = Box(""),
    val count: Box<Int> = Box(0),
    val nested: Box<List<Box<Boolean>>> = Box(emptyList()),
)
