package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * Every collection type produces an array, mutable or not.
 */
@SchemaRoot
data class Containers(
    val array: Array<String> = emptyArray(),
    val list: List<String> = emptyList(),
    val mutableList: MutableList<String> = mutableListOf(),
    val set: Set<Int> = emptySet(),
    val mutableSet: MutableSet<Int> = mutableSetOf(),
    val collection: Collection<Boolean> = emptyList(),
    val mutableCollection: MutableCollection<Boolean> = mutableListOf(),
    val iterable: Iterable<Double> = emptyList(),
    val mutableIterable: MutableIterable<Double> = mutableListOf(),
)
