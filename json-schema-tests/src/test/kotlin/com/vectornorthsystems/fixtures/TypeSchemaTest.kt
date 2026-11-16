package com.vectornorthsystems.fixtures

import com.vectornorthsystems.assertSchemas
import kotlin.test.Test

/**
 * How Kotlin types map to JSON schema types.
 */
class TypeSchemaTest {

    @Test
    fun `nullable enums list null among their values, wherever they appear`() {
        // `null` is both a type and an enum value of nullable enums, in properties and array items; a
        // nullable list of enums allows null for the list, not its items.
        assertSchemas(name = "UnitPreferences", schemaOf = UnitPreferencesSchema::getOrNull)
    }

    @Test
    fun `nullable enums list null among their values as map values`() {
        // `additionalProperties` holds the enum with `null`. Gemini only, since OpenAI doesn't support maps.
        assertSchemas(name = "UnitsBySite", schemaOf = UnitsBySiteSchema::getOrNull)
    }

    @Test
    fun `nullable types allow null, independently of being required`() {
        // Every nullable property has `null` in its type; `bio` and `website` are still required, having no default.
        assertSchemas(name = "Profile", schemaOf = ProfileSchema::getOrNull)
    }

    @Test
    fun `chars, unsigned numbers and primitive arrays`() {
        // A char is a string, an unsigned number an integer with a `minimum` of 0, and primitive arrays are arrays.
        assertSchemas(name = "Measurements", schemaOf = MeasurementsSchema::getOrNull)
    }

    @Test
    fun `value classes are described by the value they wrap`() {
        // Each value class is the string or integer it wraps, also when nullable or in a list.
        assertSchemas(name = "Subscriber", schemaOf = SubscriberSchema::getOrNull)
    }

    @Test
    fun `value classes wrapping a value class or an object are unwrapped`() {
        // `handle` is a string, and `home` and `previous` reference `Residence`, defined once in `$defs`.
        assertSchemas(name = "Resident", schemaOf = ResidentSchema::getOrNull)
    }

    @Test
    fun `mutable and read-only collections are arrays`() {
        // Every kind of collection is an array of its element type.
        assertSchemas(name = "Containers", schemaOf = ContainersSchema::getOrNull)
    }

    @Test
    fun `maps express the possibility of additional properties`() {
        // Each map is an object whose `additionalProperties` is the value type.
        // Gemini only, since OpenAI doesn't support maps.
        assertSchemas(name = "Additional", schemaOf = AdditionalSchema::getOrNull)
    }
}
