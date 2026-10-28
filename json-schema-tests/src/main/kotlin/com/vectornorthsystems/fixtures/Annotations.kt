package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.*

@SchemaRoot
data class Event(
    @SchemaTitle("Name")
    @SchemaDescription($$"Costs $5 \"or\" less")
    val name: String,

    val notes: String = "",

    @SchemaFormat(SchemaFormat.Format.DATE_TIME)
    val startsAt: String,

    @SchemaMinimum(1.0)
    @SchemaMaximum(4.5)
    val rating: Double = 1.0,

    @SchemaMinimum(0.0)
    val attendees: Int = 0,

    @SchemaMinItems(1)
    @SchemaMaxItems(20)
    val speakers: List<String> = emptyList(),

    @SchemaExclude
    val startTime: String = "",

    @SchemaExclude
    val endTime: String = "",
)
