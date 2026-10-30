package com.vectornorthsystems.fixtures

import com.vectornorthsystems.assertSchemas
import kotlin.test.Test

/**
 * How Kotlin types map to JSON schema types.
 */
class TypeSchemaTest {

    @Test
    fun `nullable enums list null among their values, wherever they appear`() {
        // `null` is both a type and an enum value of nullable enums, in properties, array items and map values; a
        // nullable list of enums allows null for the list, not its items.
        assertSchemas("UnitPreferences", UnitPreferencesSchema::getOrNull)
    }

    @Test
    fun `nullable types allow null, independently of being required`() {
        // Every nullable property has `null` in its type; `bio` and `website` are still required, having no default.
        assertSchemas("Profile", ProfileSchema::getOrNull)
    }

    @Test
    fun `chars, unsigned numbers and primitive arrays`() {
        // A char is a string, an unsigned number an integer with a `minimum` of 0, and primitive arrays are arrays.
        assertSchemas("Measurements", MeasurementsSchema::getOrNull)
    }

    @Test
    fun `value classes are described by the value they wrap`() {
        // Each value class is the string or integer it wraps, also when nullable or in a list.
        assertSchemas("Subscriber", SubscriberSchema::getOrNull)
    }

    @Test
    fun `value classes wrapping a value class or an object are unwrapped`() {
        // `handle` is a string, and `home` and `previous` reference `Residence`, defined once in `$defs`.
        assertSchemas("Resident", ResidentSchema::getOrNull)
    }

    @Test
    fun `mutable and read-only collections are arrays`() {
        // Every kind of collection is an array of its element type.
        assertSchemas("Containers", ContainersSchema::getOrNull)
    }

    @Test
    fun `maps express the possibility of additional properties`() {
        // Each map is an object whose `additionalProperties` is the value type.
        assertSchemas("Additional", AdditionalSchema::getOrNull)
    }
}
