package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.vectornorthsystems.jsonschema.SchemaTarget
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationProcessorTest {

    private val logger = mockk<KSPLogger>(relaxed = true)
    private val processor = ConfigurationProcessor(logger)

    // region options

    @Test
    fun `an unknown option is a warning listing the valid ones`() {
        // Given a misspelled option:
        val options = mapOf("jsonSchema.target" to "gemini")

        // When processing the options...
        val configuration = processor.process(options)

        // We expect a warning, not an error, and the defaults kept:
        verify {
            logger.warn(
                "Unknown option 'jsonSchema.target' is ignored. Valid options are: [jsonSchema.targets]",
                null
            )
        }
        verify(exactly = 0) { logger.error(any(), any()) }
        assertEquals(SchemaTarget.entries.toSet(), configuration.dialects)
    }

    @Test
    fun `only the exact prefix is stripped from an option`() {
        // Given an option that ends like a known one but isn't:
        val options = mapOf("jsonSchema.other.targets" to "nope")

        // When processing the options...
        val configuration = processor.process(options)

        // We expect it to be unknown, not read as the targets:
        verify { logger.warn(match { it.startsWith("Unknown option 'jsonSchema.other.targets'") }, null) }
        verify(exactly = 0) { logger.error(any(), any()) }
        assertEquals(SchemaTarget.entries.toSet(), configuration.dialects)
    }

    @Test
    fun `options of other processors are ignored silently`() {
        // Given options without the prefix:
        val options = mapOf("room.schemaLocation" to "schemas", "targets" to "nope")

        // When processing the options...
        processor.process(options)

        // We expect neither warnings nor errors:
        verify(exactly = 0) { logger.warn(any(), any()) }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    // endregion

    // region targets

    @Test
    fun `an empty targets option is an error and keeps the defaults`() {
        // Given a targets option with only commas and blanks:
        val options = mapOf("jsonSchema.targets" to " , ")

        // When processing the options...
        val configuration = processor.process(options)

        // We expect an error, and every target:
        verify { logger.error(match { it.startsWith("No targets set") }, null) }
        assertEquals(SchemaTarget.entries.toSet(), configuration.dialects)
    }

    @Test
    fun `an unknown target is an error listing the valid ones`() {
        // Given a known and an unknown target:
        val options = mapOf("jsonSchema.targets" to "gemini,openai")

        // When processing the options...
        val configuration = processor.process(options)

        // We expect an error naming the unknown target and the valid ones, and the defaults kept:
        verify { logger.error("Unknown targets [openai] in 'jsonSchema.targets'. Valid targets are: [gemini]", null) }
        assertEquals(SchemaTarget.entries.toSet(), configuration.dialects)
    }

    @Test
    fun `without a targets option every target is generated`() {
        // Given no options:
        val options = emptyMap<String, String>()

        // When processing the options...
        val configuration = processor.process(options)

        // We expect every target, and no errors:
        assertEquals(SchemaTarget.entries.toSet(), configuration.dialects)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `targets ignore case and blanks, and repeated targets count once`() {
        // Given targets in mixed case, padded, repeated and with an empty entry:
        val options = mapOf("jsonSchema.targets" to " Gemini , GEMINI,,gemini ")

        // When processing the options...
        val configuration = processor.process(options)

        // We expect a single target, and no errors:
        assertEquals(setOf(SchemaTarget.GEMINI), configuration.dialects)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `the targets option replaces the defaults`() {
        // Given a targets option:
        val options = mapOf("jsonSchema.targets" to "gemini")

        // When processing the options...
        val configuration = processor.process(options)

        // We expect only the listed targets, not the defaults plus them:
        assertEquals(setOf(SchemaTarget.GEMINI), configuration.dialects)
    }

    // endregion
}
