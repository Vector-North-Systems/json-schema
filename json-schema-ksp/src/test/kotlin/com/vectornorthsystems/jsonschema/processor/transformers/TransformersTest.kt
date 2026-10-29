package com.vectornorthsystems.jsonschema.processor.transformers

import com.vectornorthsystems.jsonschema.SchemaTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

class TransformersTest {

    @Test
    fun `each call gives a new transformer`() {
        // Given a dialect...
        // When asking for its transformer twice...
        val first = SchemaTarget.GEMINI.transformer()
        val second = SchemaTarget.GEMINI.transformer()

        // We expect two instances, since a transformer remembers the objects of the one tree it copied:
        assertNotSame(first, second)
    }

    @Test
    fun `every dialect gets its own transformer`() {
        // Given every dialect:
        SchemaTarget.entries.forEach { target ->
            // When asking for its transformer...
            val transformer = target.transformer()

            // We expect one that lowers for that dialect:
            assertEquals(target, transformer.target)
        }

        // And Gemini's to be the Gemini transformer:
        assertIs<GeminiTransformer>(SchemaTarget.GEMINI.transformer())
    }
}
