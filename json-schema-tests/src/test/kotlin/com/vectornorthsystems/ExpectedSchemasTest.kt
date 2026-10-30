package com.vectornorthsystems

import com.vectornorthsystems.jsonschema.SchemaTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the expected schemas themselves, which [assertSchemas] can't.
 */
class ExpectedSchemasTest {

    private val root = File(SchemaResources::class.java.classLoader.getResource(EXPECTED_SCHEMAS)!!.toURI())

    @Test
    fun `every directory is a dialect`() {
        // Given the directories of expected schemas...
        val directories = root.listFiles()!!.map { it.name }.toSet()

        // When comparing them with the dialects...
        val dialects = SchemaTarget.entries.map { it.name.lowercase() }.toSet()

        // We expect no directory that isn't a dialect:
        assertTrue(dialects.containsAll(directories), "Unknown dialect directories in $root: ${directories - dialects}")
    }

    @Test
    fun `every expected schema is a JSON object in a json file`() {
        // Given every file in the dialect directories...
        val files = root.listFiles()!!.flatMap { it.listFiles()!!.toList() }

        // When reading them...
        // We expect each to be a .json file with a JSON object:
        assertTrue(files.isNotEmpty(), "No expected schemas in $root")
        files.forEach { file ->
            assertEquals("json", file.extension, "$file isn't a .json file")
            assertTrue(
                runCatching { Json.parseToJsonElement(file.readText()) is JsonObject }
                    .getOrDefault(false),
                "$file isn't a JSON object"
            )
        }
    }
}
