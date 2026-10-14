package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot


@SchemaRoot
data class Recipe(
    val name: String,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
)

/**
 * Enums are generated as its Kotlin string name.
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL
}

data class Instruction(
    val stepNumber: Int = 0,
    val description: String = "",
)

data class Ingredient(
    val name: String,
    val quantity: Quantity,
    val optional: Boolean = false
)

data class Quantity(
    val amount: Double,
    val unit: String = "",
)
