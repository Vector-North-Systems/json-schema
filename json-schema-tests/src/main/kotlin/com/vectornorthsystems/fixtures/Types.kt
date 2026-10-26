package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaMaximum
import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * Kotlin types beyond the common primitives.
 * Some of questionable usefulness, yet straightforward to support.
 */
@SchemaRoot
data class Measurements(
    val grade: Char = 'A',
    @SchemaMaximum(10.0)
    val level: UInt = 0u,
    // In real code, equals and hashCode should be overridden here for correctness.
    val samples: IntArray = intArrayOf(),
    val flags: BooleanArray = booleanArrayOf(),
)

@JvmInline
value class UserId(val value: String)

@JvmInline
value class Wrapper<T>(val value: T)

/**
 * Value classes are serialized as the value they wrap.
 */
@SchemaRoot
data class Subscriber(
    val id: UserId,
    val previous: UserId? = null,
    val ids: List<UserId> = emptyList(),
    val count: Wrapper<Int> = Wrapper(0),
)

@JvmInline
value class Handle(val id: UserId)

data class Residence(val city: String, val zip: String)

@JvmInline
value class Home(val address: Residence)

/**
 * Value classes wrapping another value class, or an object.
 */
@SchemaRoot
data class Resident(
    val handle: Handle,
    val home: Home,
    val previous: Home? = null,
)

/**
 * The generated function takes the visibility of the class: internal here.
 */
@SchemaRoot
internal data class Secret(val token: String)

/**
 * A private class gets an internal function, since a private one in its own file couldn't be called.
 */
@SchemaRoot
private data class Hidden(val key: String)
