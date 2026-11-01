package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.doubles.ksClassDeclaration
import com.vectornorthsystems.jsonschema.processor.doubles.ksDeclaration
import com.vectornorthsystems.jsonschema.processor.doubles.ksProperty
import com.vectornorthsystems.jsonschema.processor.doubles.ksType
import com.vectornorthsystems.jsonschema.processor.doubles.ksTypeAlias
import com.vectornorthsystems.jsonschema.processor.doubles.ksTypeParameter
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeClassifierImplTest {

    private lateinit var classifier: TypeClassifierImpl

    @BeforeEach
    fun setUp() {
        classifier = TypeClassifierImpl()
    }

    // region Unsupported

    @Test
    fun `a type parameter is unsupported`() {
        // Given a type that is declared by a type parameter:
        val type = ksType(ksTypeParameter("T"))

        // When classifying...
        val shape = classifier.classify(type)

        // We expect it to be unsupported, since there's no type argument to describe:
        assertEquals(
            TypeShape.Unsupported("The type parameter T has no type argument to describe; use a concrete type."),
            shape
        )
    }

    @Test
    fun `a collection without a resolvable element type is unsupported`() {
        // Given a list without type arguments, and a list with a star projection:
        val withoutArguments = ksType("kotlin.collections.List")
        val starProjected = ksType("kotlin.collections.List", null)

        // When classifying both...
        val shapes = listOf(withoutArguments, starProjected).map { classifier.classify(it) }

        // We expect both to be unsupported, since the element type can't be resolved:
        assertEquals(
            listOf(
                TypeShape.Unsupported("cannot resolve the element type of $withoutArguments"),
                TypeShape.Unsupported("cannot resolve the element type of $starProjected"),
            ),
            shapes
        )
    }

    @Test
    fun `a map without a resolvable key or value type is unsupported`() {
        // Given maps without type arguments, with the key only, and with a star projected value:
        val string = ksType("kotlin.String")
        val types = listOf(
            ksType("kotlin.collections.Map"),
            ksType("kotlin.collections.Map", string),
            ksType("kotlin.collections.Map", string, null),
            ksType("kotlin.collections.Map", null, string),
        )

        // When classifying all of them...
        val shapes = types.map { classifier.classify(it) }

        // We expect each to be unsupported, since the key and value types can't be resolved:
        assertEquals(
            types.map { TypeShape.Unsupported("cannot resolve the key and value types of $it") },
            shapes
        )
    }

    @Test
    fun `Any is unsupported`() {
        // Given the type Any:
        val type = ksType("kotlin.Any")

        // When classifying...
        val shape = classifier.classify(type)

        // We expect it to be unsupported, since it could be anything:
        assertEquals(TypeShape.Unsupported("Any could be anything; use a concrete type"), shape)
    }

    @Test
    fun `a declaration that is not a class is unsupported`() {
        // Given a type whose declaration is not a class, such as a function:
        val type = ksType(ksDeclaration("com.example.Thing"))

        // When classifying...
        val shape = classifier.classify(type)

        // We expect it to be unsupported:
        assertEquals(TypeShape.Unsupported("$type isn't a class"), shape)
    }

    @Test
    fun `interfaces, sealed and abstract classes are unsupported`() {
        // Given an interface, a sealed class and an abstract class:
        val types = listOf(
            ksType(ksClassDeclaration(kind = ClassKind.INTERFACE)),
            ksType(ksClassDeclaration(modifiers = setOf(Modifier.SEALED))),
            ksType(ksClassDeclaration(modifiers = setOf(Modifier.ABSTRACT))),
        )

        // When classifying all of them...
        val shapes = types.map { classifier.classify(it) }

        // We expect each to be unsupported:
        assertEquals(
            types.map { TypeShape.Unsupported("interfaces, sealed and abstract classes like $it aren't supported yet") },
            shapes
        )
    }

    @Test
    fun `a value class without a backing field is unsupported`() {
        // Given a value class whose only property has no backing field:
        val property = ksProperty(type = ksType("kotlin.String"), hasBackingField = false)
        val type = ksType(ksClassDeclaration(modifiers = setOf(Modifier.VALUE), properties = listOf(property)))

        // When classifying...
        val shape = classifier.classify(type)

        // We expect it to be unsupported, since there's no underlying value:
        assertEquals(TypeShape.Unsupported("cannot find the underlying value of $type"), shape)
    }

    @Test
    fun `a value class without properties is unsupported`() {
        // Given a value class without properties:
        val type = ksType(ksClassDeclaration(modifiers = setOf(Modifier.VALUE)))

        // When classifying...
        val shape = classifier.classify(type)

        // We expect it to be unsupported, since there's no underlying value:
        assertEquals(TypeShape.Unsupported("cannot find the underlying value of $type"), shape)
    }

    // endregion

    // region Aliases and primitives

    @Test
    fun `a type alias is described by the type it stands for`() {
        // Given an alias of a string:
        val target = ksType("kotlin.String")
        val type = ksType(ksTypeAlias("com.example.Name", target))

        // When classifying...
        val shape = classifier.classify(type)

        // We expect the alias to hold its target, to be classified in turn:
        assertEquals(TypeShape.Alias(target), shape)
    }

    @Test
    fun `integer types are integer primitives`() {
        knownIntegerTypes.forEach { name ->
            // Given each integer type:
            val type = ksType(name)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect an integer without a minimum:
            assertEquals(TypeShape.Primitive(PrimitiveType.INTEGER), shape, name)
        }
    }

    @Test
    fun `unsigned types are integer primitives that start at zero`() {
        knownUnsignedTypes.forEach { name ->
            // Given each unsigned type:
            val type = ksType(name)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect an integer with a minimum of 0:
            assertEquals(TypeShape.Primitive(PrimitiveType.INTEGER, minimum = 0.0), shape, name)
        }
    }

    @Test
    fun `floating point types are number primitives`() {
        knownNumberTypes.forEach { name ->
            // Given each floating point type:
            val type = ksType(name)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect a number:
            assertEquals(TypeShape.Primitive(PrimitiveType.NUMBER), shape, name)
        }
    }

    @Test
    fun `Boolean is a boolean primitive`() {
        // Given a Boolean:
        val type = ksType("kotlin.Boolean")

        // When classifying...
        val shape = classifier.classify(type)

        // We expect a boolean:
        assertEquals(TypeShape.Primitive(PrimitiveType.BOOLEAN), shape)
    }

    @Test
    fun `String and Char are string primitives`() {
        knownStringTypes.forEach { name ->
            // Given each string type:
            val type = ksType(name)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect a string:
            assertEquals(TypeShape.Primitive(PrimitiveType.STRING), shape, name)
        }
    }

    @Test
    fun `nullability doesn't change the primitive`() {
        // Given a nullable Int:
        val type = ksType("kotlin.Int", nullable = true)

        // When classifying...
        val shape = classifier.classify(type)

        // We expect the same shape as the non-null type:
        assertEquals(TypeShape.Primitive(PrimitiveType.INTEGER), shape)
    }

    @Test
    fun `primitive arrays are primitive collections`() {
        knownPrimitiveArrayTypes.keys.forEach { name ->
            // Given each primitive array type:
            val type = ksType(name)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect a primitive collection of that array type:
            assertEquals(TypeShape.PrimitiveCollection(name), shape)
        }
    }

    // endregion

    // region Enums, collections and maps

    @Test
    fun `an enum is described by its entries`() {
        // Given an enum with two entries, and a nested class and a function that aren't entries:
        val declaration = ksClassDeclaration(
            kind = ClassKind.ENUM_CLASS,
            declarations = listOf(
                ksClassDeclaration(kind = ClassKind.ENUM_ENTRY, simpleName = "RED"),
                ksClassDeclaration(kind = ClassKind.CLASS, simpleName = "Helper"),
                ksDeclaration("com.example.Color.describe"),
                ksClassDeclaration(kind = ClassKind.ENUM_ENTRY, simpleName = "GREEN"),
            )
        )

        // When classifying...
        val shape = classifier.classify(ksType(declaration))

        // We expect only the entries, in declaration order:
        assertEquals(TypeShape.EnumOf(listOf("RED", "GREEN")), shape)
    }

    @Test
    fun `an enum without entries has no values`() {
        // Given an enum without entries:
        val declaration = ksClassDeclaration(kind = ClassKind.ENUM_CLASS)

        // When classifying...
        val shape = classifier.classify(ksType(declaration))

        // We expect no entries:
        assertEquals(TypeShape.EnumOf(emptyList()), shape)
    }

    @Test
    fun `collections are described by their element type`() {
        knownArrayTypes.forEach { name ->
            // Given each collection type of strings:
            val element = ksType("kotlin.String")
            val type = ksType(name, element)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect a collection of the element type:
            assertEquals(TypeShape.Collection(element), shape, name)
        }
    }

    @Test
    fun `maps are described by their key and value types`() {
        knownMapTypes.forEach { name ->
            // Given each map type from strings to integers:
            val key = ksType("kotlin.String")
            val value = ksType("kotlin.Int")
            val type = ksType(name, key, value)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect a map of the key and value types:
            assertEquals(TypeShape.MapOf(key, value), shape, name)
        }
    }

    // endregion

    // region Value classes and objects

    @Test
    fun `a value class is described by the type it wraps`() {
        listOf(Modifier.VALUE, Modifier.INLINE).forEach { modifier ->
            // Given a value class that wraps a string:
            val underlying = ksType("kotlin.String")
            val declaration = ksClassDeclaration(
                modifiers = setOf(modifier),
                properties = listOf(ksProperty(type = underlying))
            )

            // When classifying...
            val shape = classifier.classify(ksType(declaration))

            // We expect the wrapped type:
            assertEquals(TypeShape.Value(underlying), shape, modifier.name)
        }
    }

    @Test
    fun `a value class skips properties without a backing field`() {
        // Given a value class with a computed property before the one it wraps:
        val underlying = ksType("kotlin.Int")
        val declaration = ksClassDeclaration(
            modifiers = setOf(Modifier.VALUE),
            properties = listOf(
                ksProperty(type = ksType("kotlin.String"), hasBackingField = false),
                ksProperty(type = underlying),
            )
        )

        // When classifying...
        val shape = classifier.classify(ksType(declaration))

        // We expect the type of the property with the backing field:
        assertEquals(TypeShape.Value(underlying), shape)
    }

    @Test
    fun `a nullable value class is resolved as its non-null type`() {
        // Given a value class property, and a nullable type of it that makes a non-null type:
        val underlying = ksType("kotlin.String")
        val property = ksProperty(type = underlying)
        val declaration = ksClassDeclaration(modifiers = setOf(Modifier.VALUE), properties = listOf(property))
        val nonNull = ksType(declaration)
        val nullable = ksType(declaration, nullable = true)
        every { nullable.makeNotNullable() } returns nonNull

        // When classifying the nullable type...
        val shape = classifier.classify(nullable)

        // We expect the property to be read as a member of the non-null type:
        assertEquals(TypeShape.Value(underlying), shape)
        verify { property.asMemberOf(nonNull) }
    }

    @Test
    fun `classes and objects are described by their declaration`() {
        listOf(
            ksClassDeclaration(kind = ClassKind.CLASS),
            ksClassDeclaration(kind = ClassKind.CLASS, modifiers = setOf(Modifier.DATA)),
            ksClassDeclaration(kind = ClassKind.OBJECT),
        ).forEach { declaration ->
            // Given a concrete class, a data class and an object:
            val type = ksType(declaration)

            // When classifying...
            val shape = classifier.classify(type)

            // We expect an object described by the declaration:
            assertEquals(TypeShape.Object(declaration), shape)
        }
    }

    // endregion
}
