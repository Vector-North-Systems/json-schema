package com.vectornorthsystems

import com.vectornorthsystems.jsonschema.SchemaTarget
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Where the expected schemas live, one directory per dialect: `expected/{target}/{name}.json`.
 */
internal const val EXPECTED_SCHEMAS = "expected"

/**
 * The resources' directory.
 */
private const val RESOURCES = "src/test/resources"

/**
 * The expected schema's resource path for [target] and [name].
 */
internal fun expectedSchemaPath(target: SchemaTarget, name: String): String =
    "$EXPECTED_SCHEMAS/${target.name.lowercase()}/$name.json"

/**
 * Compares two schemas as JSON, ignoring formatting and the order of keys.
 */
internal fun assertSchema(expected: String, actual: String) {
    assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
}

/**
 * Compares every dialect generated for a class with its expected schema, in `expected/{target}/{name}.json`.
 *
 * The expected files and the generated dialects must match both ways: a generated dialect without a file fails, and a
 * file without a generated dialect also fails. Every dialect is checked before failing.
 *
 * @param name The name of the expected files, usually the class's simple name.
 * @param schemaOf The generated object's `getOrNull`, such as `ComplexSchema::getOrNull`.
 */
internal fun assertSchemas(name: String, schemaOf: (SchemaTarget) -> String?) {
    val failures = SchemaTarget.entries.mapNotNull { target ->
        val path = expectedSchemaPath(target, name)
        val expected = SchemaResources::class.java.classLoader.getResource(path)?.readText()
        val actual = schemaOf(target)

        when {
            expected == null && actual == null -> null
            expected == null -> "$name has a $target schema, but $RESOURCES/$path doesn't exist. Generated:\n$actual"
            actual == null -> "$RESOURCES/$path exists, but $name has no $target schema"
            else -> runCatching { assertSchema(expected, actual) }
                .exceptionOrNull()
                ?.let { "$name doesn't match $RESOURCES/$path: ${it.message}" }
        }
    }

    if (failures.isNotEmpty()) fail(failures.joinToString("\n\n"))
}

/**
 * Anchors the class loader that reads the expected schemas.
 */
internal object SchemaResources
