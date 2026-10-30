package com.vectornorthsystems.dialect.gemini

import com.vectornorthsystems.jsonschema.SchemaTarget
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The module sets `jsonSchema.targets` to `gemini`. Reaching these tests means the build accepted the option.
 */
class TargetsOptionTest {

    // TODO: When SchemaTarget has a second dialect, check that getOrNull gives null for it here, since the option
    //  leaves it out. With GEMINI alone, the schemas are the same with the option as without it.
    @Test
    fun `the targets option generates the dialects it lists`() {
        // Given a root without its own targets, in a module with `jsonSchema.targets = gemini`...
        // When picking its dialects at runtime...
        // We expect the GEMINI schema:
        assertEquals(
            """{"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}""",
            PlanetSchema.getOrNull(SchemaTarget.GEMINI)
        )
    }
}
