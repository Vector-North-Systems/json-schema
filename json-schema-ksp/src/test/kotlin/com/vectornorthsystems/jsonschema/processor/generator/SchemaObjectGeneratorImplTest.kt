package com.vectornorthsystems.jsonschema.processor.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.doubles.ksName
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaObjectGeneratorImplTest {

    private val codeGenerator = mockk<CodeGenerator>()
    private val generator = SchemaObjectGeneratorImpl(codeGenerator, mockk<KSPLogger>(relaxed = true))

    /**
     * The source of a public `StarSchema` object, in `com.example`, for the class `Star`.
     */
    private fun source(
        schemas: List<TargetSchema>,
        packageName: String = "com.example",
        internal: Boolean = false,
    ): String = generator.source(
        packageName = packageName,
        className = "Star",
        qualifiedName = if (packageName.isEmpty()) "Star" else "$packageName.Star",
        internal = internal,
        objectName = "StarSchema",
        schemas = schemas,
    )

    private val gemini = TargetSchema(SchemaTarget.GEMINI, """{"type":"object"}""")

    // region source

    @Test
    fun `dialects that aren't generated give null, and fail in get with a hint`() {
        // Given no schemas, the only way to leave dialects out while SchemaTarget has a single one:
        val schemas = emptyList<TargetSchema>()

        // When building the source...
        val source = source(schemas)

        // We expect no constants, getOrNull to give null for every dialect, and get to throw with a message that names
        // the object, the dialects it has, and where to add the missing one:
        assertEquals(
            $$"""
            package com.example

            import com.vectornorthsystems.jsonschema.SchemaTarget

            /**
             * Generated schemas for [Star], one per dialect.
             *
             * @see [com.example.Star]
             */
            object StarSchema {
                /**
                 * The schema of [target], or null if it isn't generated for [Star].
                 */
                fun getOrNull(target: SchemaTarget): String? = when (target) {
                    else -> null
                }

                /**
                 * The schema of [target].
                 *
                 * @throws IllegalArgumentException If [target] isn't generated for [Star].
                 */
                operator fun get(target: SchemaTarget): String = getOrNull(target)
                    ?: throw IllegalArgumentException(
                        "StarSchema has no $target schema, only none. Add ${target.name.lowercase()} to the " +
                            "jsonSchema.targets KSP option, or $target to @SchemaRoot(targets = ...) on Star."
                    )
            }

            """.trimIndent(),
            source
        )
    }

    @Test
    fun `dollars are escaped, so they don't start templates`() {
        // Given a schema with references, whose keywords start with `$`:
        val schemas = listOf(TargetSchema(SchemaTarget.GEMINI, $$"""{"$ref":"#/$defs/Star"}"""))

        // When building the source...
        val source = source(schemas)

        // We expect every `$` written as a template that yields it:
        val dollar = $$"${'$'}"
        val quotes = "\"\"\""
        assertTrue(
            source.contains("const val GEMINI: String = $quotes{\"${dollar}ref\":\"#/${dollar}defs/Star\"}$quotes"),
            source
        )
    }

    @Test
    fun `quotes and backslashes stay as they are in the raw string`() {
        // Given a schema whose description has escaped quotes, a backslash and a newline:
        val json = """{"description":"Say \"hi\" \\ then\nleave"}"""
        val schemas = listOf(TargetSchema(SchemaTarget.GEMINI, json))

        // When building the source...
        val source = source(schemas)

        // We expect the JSON unchanged, since a raw string doesn't process escapes:
        assertTrue(source.contains("const val GEMINI: String = \"\"\"$json\"\"\""), source)
    }

    @Test
    fun `classes in the root package have no package directive`() {
        // Given a class in the root package...
        // When building the source...
        val source = source(listOf(gemini), packageName = "")

        // We expect the file to start with the import:
        assertFalse(source.contains("package"), source)
        assertTrue(source.startsWith("import com.vectornorthsystems.jsonschema.SchemaTarget\n"), source)
        assertTrue(source.contains(" * @see [Star]\n"), source)
    }

    @Test
    fun `an internal object is marked internal`() {
        // Given an object that must be internal...
        // When building the source...
        val source = source(listOf(gemini), internal = true)

        // We expect the modifier, with a single space before `object`:
        assertTrue(source.contains("\ninternal object StarSchema {\n"), source)
    }

    @Test
    fun `every dialect generated gives a constant each and an exhaustive getOrNull`() {
        // Given a schema for every dialect:
        val schemas = listOf(gemini)

        // When building the source...
        val source = source(schemas)

        // We expect a public object with one constant per dialect, a branch for each in getOrNull with no `else`, and
        // get listing the dialects it has:
        val quotes = "\"\"\""
        assertEquals(
            $$"""
            package com.example

            import com.vectornorthsystems.jsonschema.SchemaTarget

            /**
             * Generated schemas for [Star], one per dialect.
             *
             * @see [com.example.Star]
             */
            object StarSchema {
                /**
                 * The schema for the GEMINI dialect.
                 */
                const val GEMINI: String = $$quotes{"type":"object"}$$quotes

                /**
                 * The schema of [target], or null if it isn't generated for [Star].
                 */
                fun getOrNull(target: SchemaTarget): String? = when (target) {
                    SchemaTarget.GEMINI -> GEMINI
                }

                /**
                 * The schema of [target].
                 *
                 * @throws IllegalArgumentException If [target] isn't generated for [Star].
                 */
                operator fun get(target: SchemaTarget): String = getOrNull(target)
                    ?: throw IllegalArgumentException(
                        "StarSchema has no $target schema, only GEMINI. Add ${target.name.lowercase()} to the " +
                            "jsonSchema.targets KSP option, or $target to @SchemaRoot(targets = ...) on Star."
                    )
            }

            """.trimIndent(),
            source
        )
    }

    // endregion

    // region generate

    /**
     * A class named `Star` in `com.example`, with the given [modifiers], declared in [file].
     */
    private fun star(file: KSFile, modifiers: Set<Modifier> = emptySet()): KSClassDeclaration = mockk {
        every { simpleName } returns ksName("Star")
        every { qualifiedName } returns ksName("com.example.Star")
        every { packageName } returns ksName("com.example")
        every { this@mockk.modifiers } returns modifiers
        every { containingFile } returns file
    }

    @Test
    fun `a private class gets an internal object`() {
        // Given a private class:
        val output = ByteArrayOutputStream()
        every { codeGenerator.createNewFile(any(), any(), any(), any()) } returns output
        val file = mockk<KSFile>()

        // When generating its object...
        generator.generate(star(file, setOf(Modifier.PRIVATE)), "StarSchema", listOf(gemini))

        // We expect it internal, since a private object in its own file couldn't be used:
        assertTrue(output.toString().contains("\ninternal object StarSchema {\n"), output.toString())
    }

    @Test
    fun `the object is written to its own file, depending only on the class's file`() {
        // Given a public class, and a generator that captures the file it creates:
        val output = ByteArrayOutputStream()
        val dependencies = slot<Dependencies>()
        every {
            codeGenerator.createNewFile(capture(dependencies), "com.example", "StarCraft", "kt")
        } returns output
        val file = mockk<KSFile>()

        // When generating an object with a custom, lowercase name...
        generator.generate(star(file), "starCraft", listOf(gemini))

        // We expect a file in the class's package, named after the object with a capital, that depends only on the
        // class's own file:
        assertFalse(dependencies.captured.aggregating)
        assertEquals(listOf(file), dependencies.captured.originatingFiles)

        // And the source written to it, with the object name as given:
        assertEquals(
            generator.source("com.example", "Star", "com.example.Star", false, "starCraft", listOf(gemini)),
            output.toString()
        )
    }

    // endregion
}
