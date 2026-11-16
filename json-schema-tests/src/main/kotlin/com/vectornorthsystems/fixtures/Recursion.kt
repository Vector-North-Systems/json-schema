package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTitle

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

/**
 * References with a title or a description. Some dialects reject keywords next to a `$ref`, and wrap it in `anyOf`.
 */
@SchemaRoot
data class Delivery(
    @SchemaDescription("Where the parcel is delivered")
    val destination: Depot,

    @SchemaTitle("Sender")
    @SchemaDescription("Where the parcel is sent from")
    val origin: Depot,

    // Already inside anyOf, with null.
    @SchemaDescription("Where the parcel goes back to")
    val returnTo: Depot?,

    // The description is next to the array, not to the reference.
    @SchemaDescription("The depots on the way")
    val stops: List<Depot>,

    // Nothing next to the reference.
    val customs: Depot,
)

data class Depot(
    val city: String,
)
