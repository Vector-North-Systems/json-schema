package com.vectornorthsystems.dialect.gemini

import com.vectornorthsystems.jsonschema.SchemaTarget
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The module sets `jsonSchema.targets` to `gemini`. Reaching these tests means the build accepted the option.
 */
class TargetsOptionTest {

    @Test
    fun `the targets option generates the dialects it lists`() {
        // Given a root without overridden targets, in a module with `jsonSchema.targets = gemini`...
        // When picking its dialects at runtime...
        // We expect the GEMINI schema:
        assertEquals(
            """{"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}""",
            PlanetSchema.getOrNull(SchemaTarget.GEMINI)
        )
    }

    @Test
    fun `the root option overrides the targets option dialects`() {
        // Given a root with overridden targets (See [Star]), in a module with `jsonSchema.targets = gemini`...
        // When picking its dialects at runtime...
        // We expect the overridden schemas to be present:
        assertEquals(
            """{"type":"object","properties":{"starClass":{"type":"string","enum":["O","B","A","F","G","K","M","L","T"]},"colorDescription":{"type":"string"}},"required":["starClass","colorDescription"]}""",
            StarSchema.getOrNull(SchemaTarget.GEMINI)
        )
        assertEquals(
            """{"type":"object","properties":{"starClass":{"type":"string","enum":["O","B","A","F","G","K","M","L","T"]},"colorDescription":{"type":"string"}},"additionalProperties":false,"required":["starClass","colorDescription"]}""",
            StarSchema.getOrNull(SchemaTarget.OPENAI)
        )
    }

    @Test
    fun `the targets option don't generate the dialects it doesn't list`() {
        // Given a root without its own targets, in a module with `jsonSchema.targets = gemini`...
        // When picking its dialects at runtime...
        // We expect only the GEMINI schema and not other:
        assertEquals(
            null,
            PlanetSchema.getOrNull(SchemaTarget.OPENAI)
        )
    }
}
