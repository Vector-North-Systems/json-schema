package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * The root contains itself, referenced with `#`.
 */
@SchemaRoot
data class TreeNode(
    val label: String = "",
    val children: List<TreeNode> = emptyList(),
)

/**
 * A nested class contains itself, defined once in `$defs`.
 */
@SchemaRoot
data class Organization(
    val head: Employee = Employee(),
)

data class Employee(
    val name: String = "",
    val reports: List<Employee> = emptyList(),
    val manager: Employee? = null,
)

/**
 * Identical Employee classes should be produced.
 */
@SchemaRoot
data class Org(
    val ceo: Employee,
    val cto: Employee
)