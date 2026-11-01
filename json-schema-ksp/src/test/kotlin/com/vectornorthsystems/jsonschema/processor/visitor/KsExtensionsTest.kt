package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.vectornorthsystems.jsonschema.SchemaExclude
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.doubles.ksEntry
import com.vectornorthsystems.jsonschema.processor.doubles.ksAnnotation
import com.vectornorthsystems.jsonschema.processor.doubles.ksClassDeclaration
import com.vectornorthsystems.jsonschema.processor.doubles.ksDeclaration
import com.vectornorthsystems.jsonschema.processor.doubles.ksName
import com.vectornorthsystems.jsonschema.processor.doubles.ksType
import com.vectornorthsystems.jsonschema.processor.doubles.ksParameter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.*


class KsExtensionsTest {

    // region isValidRoot

    @Test
    fun isValidRoot() {
        // Given a logger:
        val logger = mockk<KSPLogger>(relaxed = true)

        // When validating a class, an object and a data class...
        val results = listOf(
            ksClassDeclaration(ClassKind.CLASS),
            ksClassDeclaration(ClassKind.OBJECT),
            ksClassDeclaration(modifiers = setOf(Modifier.DATA)),
        ).map { it.isValidRoot(logger) }

        // We expect all of them to be valid, and no errors:
        assertEquals(listOf(true, true, true), results)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun isValidRootRejectsTypeParameters() {
        // Given a logger:
        val logger = mockk<KSPLogger>(relaxed = true)

        // And a class with a type parameter:
        val declaration = ksClassDeclaration(typeParameters = listOf(mockk()))

        // When validating...
        val valid = declaration.isValidRoot(logger)

        // We expect it to be rejected, with the error logged against the class:
        assertFalse(valid)
        verify { logger.error("@SchemaRoot classes can't have type parameters", declaration) }
    }

    @Test
    fun isValidRootRejectsNonConcreteClasses() {
        // Given declarations that aren't concrete classes:
        val invalid = listOf(
            ksClassDeclaration(ClassKind.ENUM_CLASS),
            ksClassDeclaration(ClassKind.ENUM_ENTRY),
            ksClassDeclaration(ClassKind.INTERFACE),
            ksClassDeclaration(ClassKind.ANNOTATION_CLASS),
            ksClassDeclaration(modifiers = setOf(Modifier.SEALED)),
            ksClassDeclaration(modifiers = setOf(Modifier.ABSTRACT)),
        )

        invalid.forEach { declaration ->
            // And a logger for each:
            val logger = mockk<KSPLogger>(relaxed = true)

            // When validating...
            val valid = declaration.isValidRoot(logger)

            // We expect it to be rejected, with exactly one error logged against it:
            assertFalse(valid)
            verify(exactly = 1) { logger.error(any(), declaration) }
        }
    }

    @Test
    fun isValidRootWithoutLogger() {
        // Given no logger...
        // When validating a class and an interface...
        // We expect the result to be the same, without anything to report to:
        assertTrue(ksClassDeclaration().isValidRoot(null))
        assertFalse(ksClassDeclaration(ClassKind.INTERFACE).isValidRoot(null))
    }

    // endregion

    // region shouldNotBeExcluded

    @Test
    fun getShouldNotBeExcluded() {
        // Given a way to declare properties with annotations:
        fun property(vararg annotations: KSAnnotation): KSPropertyDeclaration = mockk {
            every { this@mockk.annotations } returns annotations.asSequence()
        }

        // When checking properties without SchemaExclude...
        // We expect them to not be excluded:
        assertTrue(property().shouldNotBeExcluded)
        assertTrue(property(ksAnnotation("com.vectornorthsystems.jsonschema.SchemaRequired")).shouldNotBeExcluded)
        // Same simple name, different package.
        assertTrue(property(ksAnnotation("com.example.SchemaExclude")).shouldNotBeExcluded)

        // When checking properties with SchemaExclude, alone or among other annotations...
        // We expect them to be excluded:
        assertFalse(property(ksAnnotation(SchemaExclude::class.qualifiedName!!)).shouldNotBeExcluded)
        assertFalse(
            property(
                ksAnnotation("com.vectornorthsystems.jsonschema.SchemaRequired"),
                ksAnnotation(SchemaExclude::class.qualifiedName!!),
            ).shouldNotBeExcluded
        )
    }

    // endregion

    // region key

    @Test
    fun key() {
        // Given types without arguments, with an argument, and with nested and nullable arguments...
        // When producing their keys...
        // We expect the arguments in angle brackets, and a question mark after the nullable ones:
        assertEquals("kotlin.String", ksType("kotlin.String").key())
        assertEquals("com.example.Box<kotlin.String>", ksType("com.example.Box", ksType("kotlin.String")).key())
        assertEquals(
            "kotlin.collections.Map<kotlin.String,kotlin.Int?>",
            ksType("kotlin.collections.Map", ksType("kotlin.String"), ksType("kotlin.Int", nullable = true)).key()
        )
        assertEquals(
            "com.example.Box<kotlin.collections.List<kotlin.String?>>",
            ksType("com.example.Box", ksType("kotlin.collections.List", ksType("kotlin.String", nullable = true))).key()
        )
    }

    @Test
    fun keyOfStarProjection() {
        // Given a type with a star projection:
        val type = ksType("com.example.Box", null)

        // When producing its key...
        val key = type.key()

        // We expect an asterisk in place of the argument:
        assertEquals("com.example.Box<*>", key)
    }

    @Test
    fun keyDistinguishesTypeArguments() {
        // Given the same type with different type arguments:
        val strings = ksType("com.example.Box", ksType("kotlin.String"))
        val ints = ksType("com.example.Box", ksType("kotlin.Int"))

        // When producing their keys...
        val stringsKey = strings.key()
        val intsKey = ints.key()

        // We expect them to differ:
        assertTrue(stringsKey != intsKey)
    }

    // endregion

    // region enumEntries

    @Test
    fun enumEntries() {
        // Given an enum whose members are entries, a function and a companion:
        val enum = mockk<KSClassDeclaration> {
            every { declarations } returns sequenceOf(
                ksClassDeclaration(ClassKind.ENUM_ENTRY, simpleName = "RED"),
                mockk<KSFunctionDeclaration>(),
                ksClassDeclaration(ClassKind.ENUM_ENTRY, simpleName = "GREEN"),
                ksClassDeclaration(ClassKind.OBJECT, simpleName = "Companion"),
                ksClassDeclaration(ClassKind.ENUM_ENTRY, simpleName = "BLUE"),
            )
        }

        // When listing its entries...
        val entries = enum.enumEntries()

        // We expect only the entries, in declaration order:
        assertEquals(listOf("RED", "GREEN", "BLUE"), entries)
    }

    @Test
    fun enumEntriesOfEmptyEnum() {
        // Given an enum without members:
        val enum = mockk<KSClassDeclaration> { every { declarations } returns emptySequence() }

        // When listing its entries...
        val entries = enum.enumEntries()

        // We expect none:
        assertEquals(emptyList(), entries)
    }

    // endregion

    // region argument

    @Test
    fun argument() {
        // Given a way to declare annotation arguments:
        fun argument(name: String?, value: Any?): KSValueArgument = mockk {
            every { this@mockk.name } returns name?.let { ksName(it) }
            every { this@mockk.value } returns value
        }

        // And an annotation with an unnamed argument, two named ones, and one that is null:
        val annotation = mockk<KSAnnotation> {
            every { arguments } returns listOf(
                argument(null, "unnamed"),
                argument("value", 3),
                argument("format", "email"),
                argument("empty", null),
            )
        }

        // When looking arguments up by name...
        // We expect the values of the named ones, and null for a null or missing one:
        assertEquals(3, annotation.argument("value"))
        assertEquals("email", annotation.argument("format"))
        assertNull(annotation.argument("empty"))
        assertNull(annotation.argument("missing"))
    }

    // endregion

    // region enumEntryName

    @Test
    fun `enum entry names come from types and declarations, anything else has none`() {
        // Given an entry as a type, as a declaration, and values that aren't entries:
        val asType = ksType(ksEntry("GEMINI"))
        val asDeclaration = ksEntry("GEMINI")

        // When reading their names...
        // We expect the entry's name for the first two, and null for the others:
        assertEquals("GEMINI", enumEntryName(asType))
        assertEquals("GEMINI", enumEntryName(asDeclaration))
        assertNull(enumEntryName("GEMINI"))
        assertNull(enumEntryName(null))
    }

    // endregion

    // region parametersWithoutDefault

    @Test
    fun `parameters without a default are the ones a decoder needs`() {
        // Given a constructor with parameters with and without defaults, and one without a name:
        val declaration = ksClassDeclaration()
        every { declaration.primaryConstructor?.parameters } returns listOf(
            ksParameter("id", hasDefault = false),
            ksParameter("name", hasDefault = true),
            ksParameter("email", hasDefault = false),
            ksParameter(null, hasDefault = false),
        )

        // When reading the parameters without a default...
        val withoutDefault = declaration.parametersWithoutDefault()

        // We expect the named ones without a default:
        assertEquals(setOf("id", "email"), withoutDefault)
    }

    @Test
    fun `a class without a primary constructor has no parameters without a default`() {
        // Given a class without a primary constructor, such as an object:
        val declaration = ksClassDeclaration(kind = ClassKind.OBJECT)
        every { declaration.primaryConstructor } returns null

        // When reading the parameters without a default...
        // We expect none:
        assertEquals(emptySet(), declaration.parametersWithoutDefault())
    }

    // endregion

    // region dialects

    /**
     * A root class whose @SchemaRoot has the given targets argument, or none at all if [targets] is null.
     */
    private fun rootWithTargets(targets: Any?): KSClassDeclaration = ksClassDeclaration(
        annotations = listOf(
            ksAnnotation(
                SchemaRoot::class.qualifiedName!!,
                if (targets == null) emptyMap() else mapOf("targets" to targets)
            )
        )
    )

    @Test
    fun `an unknown target is an error pointing at the class`() {
        // Given a root asking for a target this processor doesn't know, next to a known one:
        val logger = mockk<KSPLogger>(relaxed = true)
        val root = rootWithTargets(arrayOf(ksEntry("GEMINI"), ksEntry("OPENAI")))

        // When reading its targets...
        val targets = root.dialects(logger)

        // We expect an error at the class naming the unknown target:
        verify {
            logger.error("@SchemaRoot(targets) has an unknown target 'OPENAI'. Valid targets are: [GEMINI]", root)
        }
        assertEquals(setOf(SchemaTarget.GEMINI), targets)
    }

    @Test
    fun `a targets argument that can't be read is an error pointing at the class`() {
        // Given a root whose targets argument is neither a List nor an Array:
        val logger = mockk<KSPLogger>(relaxed = true)
        val root = rootWithTargets("GEMINI")

        // When reading its targets...
        val targets = root.dialects(logger)

        // We expect an error at the class, instead of silently dropping its dialects:
        verify { logger.error("@SchemaRoot(targets) can't be read from a String", root) }
        assertEquals(emptySet(), targets)
    }

    @Test
    fun `no targets argument means no override`() {
        // Given roots without a targets argument, with an empty one, and without @SchemaRoot at all:
        val logger = mockk<KSPLogger>(relaxed = true)
        val roots = listOf(rootWithTargets(null), rootWithTargets(emptyArray<Any>()), ksClassDeclaration())

        roots.forEach { root ->
            // When reading their targets...
            // We expect none, so the module defaults apply, and no errors:
            assertEquals(emptySet(), root.dialects(logger))
        }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `targets are read from enum entries given as declarations or types`() {
        // Given targets as an array of declarations, as a list, and as a type (older KSP versions):
        val logger = mockk<KSPLogger>(relaxed = true)
        val asType = ksType(ksEntry("GEMINI"))
        val roots = listOf(
            rootWithTargets(arrayOf(ksEntry("GEMINI"))),
            rootWithTargets(listOf(ksEntry("GEMINI"))),
            rootWithTargets(arrayOf(asType)),
        )

        roots.forEach { root ->
            // When reading their targets...
            // We expect the target, and no errors:
            assertEquals(setOf(SchemaTarget.GEMINI), root.dialects(logger))
        }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    // endregion

    // region isMap and isArray

    @Test
    fun isMap() {
        // Given declarations of the known map types...
        // When checking them...
        // We expect them to be maps:
        assertTrue(ksDeclaration("kotlin.collections.Map").isMap())
        assertTrue(ksDeclaration("kotlin.collections.MutableMap").isMap())

        // Given a list, a Java map and a declaration without a name...
        // We expect them not to be maps:
        assertFalse(ksDeclaration("kotlin.collections.List").isMap())
        assertFalse(ksDeclaration("java.util.HashMap").isMap())
        assertFalse(ksDeclaration(null).isMap())
    }

    @Test
    fun isArray() {
        // Given declarations of the known array types...
        // When checking them...
        // We expect them to be arrays:
        listOf(
            "kotlin.Array",
            "kotlin.collections.List",
            "kotlin.collections.Set",
            "kotlin.collections.Collection",
            "kotlin.collections.Iterable",
            "kotlin.collections.MutableList",
            "kotlin.collections.MutableSet",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.MutableIterable",
        ).forEach { assertTrue(ksDeclaration(it).isArray(), "$it should be an array") }

        // Given a map, a primitive array and a declaration without a name...
        // We expect them not to be arrays:
        assertFalse(ksDeclaration("kotlin.collections.Map").isArray())
        assertFalse(ksDeclaration("kotlin.IntArray").isArray()) // Handled by knownPrimitiveArrayTypes.
        assertFalse(ksDeclaration(null).isArray())
    }

    // endregion
}
