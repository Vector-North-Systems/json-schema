package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertIs

class SchemaProcessorProviderTest {

    private val logger = mockk<KSPLogger>(relaxed = true)

    private fun environment(options: Map<String, String>): SymbolProcessorEnvironment = mockk {
        every { this@mockk.options } returns options
        every { this@mockk.logger } returns this@SchemaProcessorProviderTest.logger
        every { codeGenerator } returns mockk()
    }

    @Test
    fun `the provider creates the schema processor`() {
        // Given an environment without options...
        // When creating the processor...
        val processor = SchemaProcessorProvider().create(environment(emptyMap()))

        // We expect the schema processor, and no errors:
        assertIs<SchemaProcessor>(processor)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `the provider reads the KSP options`() {
        // Given an environment with an unknown target...
        // When creating the processor...
        SchemaProcessorProvider().create(environment(mapOf("jsonSchema.targets" to "nope")))

        // We expect the options to be read, and the unknown target reported with the environment's logger:
        verify { logger.error(match { it.startsWith("Unknown targets [nope]") }, null) }
    }
}
