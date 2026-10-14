package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.vectornorthsystems.jsonschema.SchemaExclude
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jdk.internal.net.http.frame.Http2Frame.asString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun name(value: String): KSName = mockk { every { asString() } returns value }

private fun declaration(qualifiedName: String?): KSDeclaration = mockk {
    every { this@mockk.qualifiedName } returns qualifiedName?.let { name(it) }
}

private fun classDeclaration(
    kind: ClassKind = ClassKind.CLASS,
    modifiers: Set<Modifier> = emptySet(),
    typeParameters: List<KSTypeParameter> = emptyList(),
    simpleName: String = "Test",
): KSClassDeclaration = mockk {
    every { classKind } returns kind
    every { this@mockk.modifiers } returns modifiers
    every { this@mockk.typeParameters } returns typeParameters
    every { this@mockk.simpleName } returns name(simpleName)
}

private fun annotation(qualifiedName: String): KSAnnotation = mockk {
    every { shortName } returns name(qualifiedName.substringAfterLast('.'))
    every { annotationType.resolve().declaration } returns declaration(qualifiedName)
}

private fun type(qualifiedName: String, vararg arguments: KSType?, nullable: Boolean = false): KSType = mockk {
    every { declaration } returns declaration(qualifiedName)
    every { isMarkedNullable } returns nullable
    every { this@mockk.arguments } returns arguments.map { argument ->
        mockk<KSTypeArgument> {
            every { type } returns argument?.let { resolved -> mockk { every { resolve() } returns resolved } }
        }
    }
}

class KsExtensionsTest {
    @Test
    fun isValidRoot() {
        val logger = mockk<KSPLogger>(relaxed = true)

        assertTrue(classDeclaration(ClassKind.CLASS).isValidRoot(logger))
        assertTrue(classDeclaration(ClassKind.OBJECT).isValidRoot(logger))
        assertTrue(classDeclaration(modifiers = setOf(Modifier.DATA)).isValidRoot(logger))

        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun isValidRootRejectsTypeParameters() {
        val logger = mockk<KSPLogger>(relaxed = true)
        val declaration = classDeclaration(typeParameters = listOf(mockk()))

        assertFalse(declaration.isValidRoot(logger))
        verify { logger.error("@SchemaRoot classes can't have type parameters", declaration) }
    }

    @Test
    fun isValidRootRejectsNonConcreteClasses() {
        val invalid = listOf(
            classDeclaration(ClassKind.ENUM_CLASS),
            classDeclaration(ClassKind.ENUM_ENTRY),
            classDeclaration(ClassKind.INTERFACE),
            classDeclaration(ClassKind.ANNOTATION_CLASS),
            classDeclaration(modifiers = setOf(Modifier.SEALED)),
            classDeclaration(modifiers = setOf(Modifier.ABSTRACT)),
        )

        invalid.forEach { declaration ->
            val logger = mockk<KSPLogger>(relaxed = true)
            assertFalse(declaration.isValidRoot(logger))
            verify(exactly = 1) { logger.error(any(), declaration) }
        }
    }

    @Test
    fun isValidRootWithoutLogger() {
        assertTrue(classDeclaration().isValidRoot(null))
        assertFalse(classDeclaration(ClassKind.INTERFACE).isValidRoot(null))
    }

    @Test
    fun getShouldNotBeExcluded() {
        fun property(vararg annotations: KSAnnotation): KSPropertyDeclaration = mockk {
            every { this@mockk.annotations } returns annotations.asSequence()
        }

        assertTrue(property().shouldNotBeExcluded)
        assertTrue(property(annotation("com.vectornorthsystems.jsonschema.SchemaRequired")).shouldNotBeExcluded)
        // Same simple name, different package.
        assertTrue(property(annotation("com.example.SchemaExclude")).shouldNotBeExcluded)

        assertFalse(property(annotation(SchemaExclude::class.qualifiedName!!)).shouldNotBeExcluded)
        assertFalse(
            property(
                annotation("com.vectornorthsystems.jsonschema.SchemaRequired"),
                annotation(SchemaExclude::class.qualifiedName!!),
            ).shouldNotBeExcluded
        )
    }

    @Test
    fun key() {
        assertEquals("kotlin.String", type("kotlin.String").key())
        assertEquals("com.example.Box<kotlin.String>", type("com.example.Box", type("kotlin.String")).key())
        assertEquals(
            "kotlin.collections.Map<kotlin.String,kotlin.Int?>",
            type("kotlin.collections.Map", type("kotlin.String"), type("kotlin.Int", nullable = true)).key()
        )
        assertEquals(
            "com.example.Box<kotlin.collections.List<kotlin.String?>>",
            type("com.example.Box", type("kotlin.collections.List", type("kotlin.String", nullable = true))).key()
        )
    }

    @Test
    fun keyOfStarProjection() {
        assertEquals("com.example.Box<*>", type("com.example.Box", null).key())
    }

    @Test
    fun keyDistinguishesTypeArguments() {
        val strings = type("com.example.Box", type("kotlin.String")).key()
        val ints = type("com.example.Box", type("kotlin.Int")).key()

        assertTrue(strings != ints)
    }

    @Test
    fun enumEntries() {
        val enum = mockk<KSClassDeclaration> {
            every { declarations } returns sequenceOf(
                classDeclaration(ClassKind.ENUM_ENTRY, simpleName = "RED"),
                mockk<KSFunctionDeclaration>(),
                classDeclaration(ClassKind.ENUM_ENTRY, simpleName = "GREEN"),
                classDeclaration(ClassKind.OBJECT, simpleName = "Companion"),
                classDeclaration(ClassKind.ENUM_ENTRY, simpleName = "BLUE"),
            )
        }

        assertEquals(listOf("RED", "GREEN", "BLUE"), enum.enumEntries())
    }

    @Test
    fun enumEntriesOfEmptyEnum() {
        val enum = mockk<KSClassDeclaration> { every { declarations } returns emptySequence() }

        assertEquals(emptyList(), enum.enumEntries())
    }

    @Test
    fun argument() {
        fun argument(name: String?, value: Any?): KSValueArgument = mockk {
            every { this@mockk.name } returns name?.let { name(it) }
            every { this@mockk.value } returns value
        }

        val annotation = mockk<KSAnnotation> {
            every { arguments } returns listOf(
                argument(null, "unnamed"),
                argument("value", 3),
                argument("format", "email"),
                argument("empty", null),
            )
        }

        assertEquals(3, annotation.argument("value"))
        assertEquals("email", annotation.argument("format"))
        assertNull(annotation.argument("empty"))
        assertNull(annotation.argument("missing"))
    }

    @Test
    fun isMap() {
        assertTrue(declaration("kotlin.collections.Map").isMap())
        assertTrue(declaration("kotlin.collections.MutableMap").isMap())

        assertFalse(declaration("kotlin.collections.List").isMap())
        assertFalse(declaration("java.util.HashMap").isMap())
        assertFalse(declaration(null).isMap())
    }

    @Test
    fun isArray() {
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
        ).forEach { assertTrue(declaration(it).isArray(), "$it should be an array") }

        assertFalse(declaration("kotlin.collections.Map").isArray())
        assertFalse(declaration("kotlin.IntArray").isArray()) // Handled by knownPrimitiveArrayTypes.
        assertFalse(declaration(null).isArray())
    }
}
