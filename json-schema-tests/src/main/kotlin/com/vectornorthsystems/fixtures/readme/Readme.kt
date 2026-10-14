package com.vectornorthsystems.fixtures.readme

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaFormat
import com.vectornorthsystems.jsonschema.SchemaMaxItems
import com.vectornorthsystems.jsonschema.SchemaMaximum
import com.vectornorthsystems.jsonschema.SchemaMinItems
import com.vectornorthsystems.jsonschema.SchemaMinimum
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTitle

// The examples in the README, kept here so they're known to compile and produce what the README shows.

@SchemaRoot
data class Complex(
    val real: Double,
    val imaginary: Double,
)

data class Ingredient(
    val name: String
)

@SchemaRoot
data class Recipe(
    val instructions: String,
    val ingredients: List<Ingredient>,
    val isColdDish: Boolean? = null
)

@SchemaRoot
data class Event(
    @SchemaTitle("Name")
    @SchemaDescription("The name of the event, as advertised")
    val name: String,

    val notes: String? = null,

    @SchemaFormat(SchemaFormat.Format.DATE_TIME)
    val startsAt: String,

    @SchemaMinimum(1.0)
    @SchemaMaximum(4.5)
    val rating: Double,

    @SchemaMinimum(0.0)
    val attendees: Int = 0,

    @SchemaMinItems(1)
    @SchemaMaxItems(20)
    val speakers: List<String>
)
