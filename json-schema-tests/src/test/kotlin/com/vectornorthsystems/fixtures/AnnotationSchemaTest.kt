package com.vectornorthsystems.fixtures

import com.vectornorthsystems.assertSchemas
import kotlin.test.Test

/**
 * The keywords the schema annotations add.
 */
class AnnotationSchemaTest {

    @Test
    fun `annotations add their keywords`() {
        // `title` and a `description` with `$` and quotes, `format`, `minimum`/`maximum` and `minItems`/`maxItems`.
        assertSchemas("Event", EventSchema::getOrNull)
    }
}
