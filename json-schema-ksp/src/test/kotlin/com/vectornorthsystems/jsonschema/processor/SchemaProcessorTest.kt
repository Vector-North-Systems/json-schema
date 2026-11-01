package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.visitor.KSValidateVisitor
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.doubles.StructScope
import com.vectornorthsystems.jsonschema.processor.doubles.ksAnnotation
import com.vectornorthsystems.jsonschema.processor.doubles.ksEntry
import com.vectornorthsystems.jsonschema.processor.doubles.struct
import com.vectornorthsystems.jsonschema.processor.generator.SchemaObjectGenerator
import com.vectornorthsystems.jsonschema.processor.generator.TargetSchema
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.visitor.SchemaVisitor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The processor with a mocked [SchemaObjectGenerator], to see what it's asked to generate. The roots are described
 * with [struct] (a Mock), and the visitor, transformers and writer run for real. Most roots have no properties, since the
 * schemas themselves are covered by their own tests.
 */
class SchemaProcessorTest {

    private val logger = mockk<KSPLogger>(relaxed = true)
    private val generator = mockk<SchemaObjectGenerator>(relaxed = true)

    /**
     * The schema of a class without properties, as every dialect writes it today.
     */
    private val emptyObject = """{"type":"object","properties":{}}"""

    private fun processor(dialects: Set<SchemaTarget> = SchemaTarget.entries.toSet()) =
        SchemaProcessor(generator, Configuration(dialects), logger)

    /**
     * A resolver that finds [symbols] annotated with [SchemaRoot].
     */
    private fun resolver(vararg symbols: KSAnnotated): Resolver = mockk {
        every { getSymbolsWithAnnotation(getQualifiedName<SchemaRoot>(), any()) } returns symbols.asSequence()
    }

    /**
     * A class annotated with [SchemaRoot] and the given [arguments], with the properties declared in [init].
     *
     * @param valid Whether KSP can resolve every type the class references, or it's deferred to the next round.
     */
    private fun root(
        simpleName: String,
        packageName: String = "com.example",
        qualifiedName: String = if (packageName.isEmpty()) simpleName else "$packageName.$simpleName",
        arguments: Map<String, Any?> = emptyMap(),
        kind: ClassKind = ClassKind.CLASS,
        valid: Boolean = true,
        init: StructScope.() -> Unit = {},
    ): KSClassDeclaration = struct(
        simpleName = simpleName,
        qualifiedName = qualifiedName,
        packageName = packageName,
        kind = kind,
        annotations = listOf(ksAnnotation(getQualifiedName<SchemaRoot>(), arguments)),
        init = init,
    ).declaration.also { declaration ->
        // KSP's validate() is a visit, and so is building the tree.
        every { declaration.accept(ofType<KSValidateVisitor>(), isNull()) } returns valid
        every { declaration.accept(ofType<SchemaVisitor>(), any<Node>()) } answers {
            firstArg<SchemaVisitor>().visitClassDeclaration(declaration, secondArg())
        }
    }

    // region Pipeline

    @Test
    fun `a root gets an object named after it, with a schema per dialect`() {
        // Given a root, in a module with every dialect...
        val star = root("Star")

        // When processing...
        val deferred = processor().process(resolver(star))

        // We expect its object generated with the default name, a schema for every dialect in their order, and
        // nothing deferred:
        verify {
            generator.generate(star, "StarSchema", SchemaTarget.entries.map { TargetSchema(it, emptyObject) })
        }
        assertEquals(emptyList(), deferred)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `a root's properties go through every step into its schema`() {
        // Given a root with a required integer and an optional list of strings:
        val point = root("Point") {
            property("x") { primitive(PrimitiveType.INTEGER) }
            property("tags", hasDefault = true) { list(primitive(PrimitiveType.STRING)) }
        }

        // When processing...
        processor(dialects = setOf(SchemaTarget.GEMINI)).process(resolver(point))

        // We expect the schema of both properties, with only the integer required:
        val schema =
            """{"type":"object","properties":{"x":{"type":"integer"},"tags":{"type":"array","items":{"type":"string"}}},""" +
                    """"required":["x"]}"""
        verify { generator.generate(point, "PointSchema", listOf(TargetSchema(SchemaTarget.GEMINI, schema))) }
    }

    @Test
    fun `roots that can't be resolved yet are deferred, not generated`() {
        // Given a root that references types not generated yet, and one that is ready:
        val pending = root("Pending", valid = false)
        val ready = root("Ready")

        // When processing...
        val deferred = processor().process(resolver(pending, ready))

        // We expect the pending root back for the next round, and only the ready one generated:
        assertEquals(listOf<KSAnnotated>(pending), deferred)
        verify(exactly = 0) { generator.generate(pending, any(), any()) }
        verify { generator.generate(ready, "ReadySchema", any()) }
    }

    @Test
    fun `invalid roots are reported and skipped`() {
        // Given an interface annotated as a root:
        val shape = root("Shape", kind = ClassKind.INTERFACE)

        // When processing...
        processor().process(resolver(shape))

        // We expect an error at it, and no object:
        verify { logger.error(match { it.startsWith("@SchemaRoot requires a concrete class or object") }, shape) }
        verify(exactly = 0) { generator.generate(any(), any(), any()) }
    }

    @Test
    fun `annotated symbols that aren't classes are ignored`() {
        // Given a function that is somehow annotated, since @SchemaRoot only targets classes:
        val function = mockk<KSFunctionDeclaration> {
            every { accept(ofType<KSValidateVisitor>(), isNull()) } returns true
        }

        // When processing...
        val deferred = processor().process(resolver(function))

        // We expect nothing generated, deferred or reported:
        verify(exactly = 0) { generator.generate(any(), any(), any()) }
        assertEquals(emptyList(), deferred)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    // endregion

    // region Dialects

    @Test
    fun `a root without targets gets the module's dialects`() {
        // Given a module with no dialects, which the option can't set but makes the defaults observable...
        val star = root("Star")

        // When processing a root without its own targets...
        processor(dialects = emptySet()).process(resolver(star))

        // We expect the module's dialects, none here:
        verify { generator.generate(star, "StarSchema", emptyList()) }
    }

    @Test
    fun `a root's own targets replace the module's dialects`() {
        // Given a module with no dialects, and a root that sets its own:
        val planet = root("Planet", arguments = mapOf("targets" to listOf(ksEntry(SchemaTarget.GEMINI.name))))

        // When processing...
        processor(dialects = emptySet()).process(resolver(planet))

        // We expect the root's dialects:
        verify { generator.generate(planet, "PlanetSchema", listOf(TargetSchema(SchemaTarget.GEMINI, emptyObject))) }
    }

    @Test
    fun `an empty targets list keeps the module's dialects`() {
        // Given a root with an empty targets list, which is the annotation's default:
        val star = root("Star", arguments = mapOf("targets" to emptyList<KSClassDeclaration>()))

        // When processing...
        processor().process(resolver(star))

        // We expect every dialect of the module:
        verify {
            generator.generate(star, "StarSchema", SchemaTarget.entries.map { TargetSchema(it, emptyObject) })
        }
    }

    // endregion

    // region Object names

    @Test
    fun `name sets the object's name`() {
        // Given a root with a name:
        val point = root("Point", arguments = mapOf("name" to "PointJsonSchema"))

        // When processing...
        processor().process(resolver(point))

        // We expect the object to have it:
        verify { generator.generate(point, "PointJsonSchema", any()) }
    }

    @Test
    fun `an empty name is the default`() {
        // Given a root with an empty name, which is the annotation's default:
        val point = root("Point", arguments = mapOf("name" to ""))

        // When processing...
        processor().process(resolver(point))

        // We expect the default name:
        verify { generator.generate(point, "PointSchema", any()) }
    }

    @Test
    fun `a name that isn't an identifier is an error`() {
        listOf("point-schema", "object", "1Point", "_").forEach { name ->
            // Given a root whose name isn't a valid identifier:
            val point = root("Point", arguments = mapOf("name" to name))

            // When processing...
            processor().process(resolver(point))

            // We expect an error at the root, and no object:
            verify { logger.error("@SchemaRoot(name = \"$name\") isn't a valid Kotlin identifier", point) }
            verify(exactly = 0) { generator.generate(point, any(), any()) }
        }
    }

    @Test
    fun `object names that differ only in case clash`() {
        // Given two roots in a package whose object names differ only in case, so they'd get the same file:
        val first = root("First", arguments = mapOf("name" to "starCraft"))
        val second = root("Second", arguments = mapOf("name" to "StarCraft"))

        // When processing both...
        processor().process(resolver(first, second))

        // We expect the first generated, and the usual error at the second instead of failing to create its file:
        verify { generator.generate(first, "starCraft", any()) }
        verify(exactly = 0) { generator.generate(second, any(), any()) }
        verify {
            logger.error(
                "StarCraft is already generated for com.example.First, set a different @SchemaRoot(name)",
                second
            )
        }
    }

    @Test
    fun `two roots with the same object name in a package is an error at the second`() {
        // Given two roots in a package, both naming their object the same:
        val first = root("First", arguments = mapOf("name" to "Shared"))
        val second = root("Second", arguments = mapOf("name" to "Shared"))

        // When processing both...
        processor().process(resolver(first, second))

        // We expect the first generated, and an error at the second naming the first:
        verify { generator.generate(first, "Shared", any()) }
        verify(exactly = 0) { generator.generate(second, any(), any()) }
        verify {
            logger.error("Shared is already generated for com.example.First, set a different @SchemaRoot(name)", second)
        }
    }

    @Test
    fun `nested classes with the same simple name clash by default`() {
        // Given two nested classes named Request, in the same package, without names:
        val search = root("Request", qualifiedName = "com.example.Search.Request")
        val login = root("Request", qualifiedName = "com.example.Login.Request")

        // When processing both...
        processor().process(resolver(search, login))

        // We expect the second to be an error, since both default to RequestSchema:
        verify { generator.generate(search, "RequestSchema", any()) }
        verify {
            logger.error(
                "RequestSchema is already generated for com.example.Search.Request, set a different @SchemaRoot(name)",
                login
            )
        }
    }

    @Test
    fun `the same name in different packages doesn't clash`() {
        // Given two roots named Point in different packages:
        val first = root("Point", packageName = "com.example.geometry")
        val second = root("Point", packageName = "com.example.charts")

        // When processing both...
        processor().process(resolver(first, second))

        // We expect both generated:
        verify { generator.generate(first, "PointSchema", any()) }
        verify { generator.generate(second, "PointSchema", any()) }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `names clash across rounds`() {
        // Given a processor that generated an object in a round:
        val processor = processor()
        val first = root("First", arguments = mapOf("name" to "Shared"))
        processor.process(resolver(first))

        // When a later round has a root with the same object name...
        val second = root("Second", arguments = mapOf("name" to "Shared"))
        processor.process(resolver(second))

        // We expect an error at it, since the first object's file already exists:
        verify(exactly = 0) { generator.generate(second, any(), any()) }
        verify { logger.error(match { it.startsWith("Shared is already generated for com.example.First") }, second) }
    }

    // endregion
}
