package com.vectornorthsystems.jsonschema.processor.doubles

import com.google.devtools.ksp.symbol.*
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import io.mockk.every
import io.mockk.mockk

/*
 * A DSL to describe Kotlin classes, built on the mocks of ksMocks.kt:
 *
 *     val address = struct("Address") {
 *         property("city") { primitive(PrimitiveType.STRING) }
 *     }
 *     val customer = struct("Customer") {
 *         property("billing") { address.type() }
 *         property("tags", hasDefault = true) { list(primitive(PrimitiveType.STRING, nullable = true)) }
 *         property("referrer", hasDefault = true) { self(nullable = true) }
 *     }
 *
 * The mocks look like what KSP yields for the same code.
 */

@DslMarker
internal annotation class KsDsl

/**
 * A class described with [struct].
 *
 * @param simpleName The written name of the class.
 * @param qualifiedName The FQN. Prepends the package to [simpleName] by default.
 * @param packageName The package, by default the [qualifiedName] without its last part; set it for nested classes.
 * @param kind The kind of class, to describe declarations that can't be roots, such as interfaces.
 * @param annotations The annotations of the class, such as `@SchemaRoot`.
 */
internal class Struct(
    simpleName: String,
    qualifiedName: String,
    packageName: String = qualifiedName.substringBeforeLast('.', missingDelimiterValue = ""),
    kind: ClassKind = ClassKind.CLASS,
    annotations: List<KSAnnotation> = emptyList(),
) {
    private val properties = mutableListOf<KSPropertyDeclaration>()
    private val parameters = mutableListOf<KSValueParameter>()

    val declaration: KSClassDeclaration = ksClassDeclaration(
        kind = kind,
        simpleName = simpleName,
        qualifiedName = qualifiedName,
        annotations = annotations,
    ).also { declaration ->
        every { declaration.packageName } returns ksName(packageName)
        // Read when visited, so properties can be added after the class is created.
        every { declaration.getAllProperties() } answers { properties.asSequence() }
        every { declaration.primaryConstructor } answers {
            mockk { every { this@mockk.parameters } returns this@Struct.parameters.toList() }
        }
    }

    /**
     * The type without type arguments, which KSP gives for the declaration itself.
     */
    private val starType: KSType = type()

    init {
        every { declaration.asStarProjectedType() } returns starType
    }

    /**
     * A type of this class, with the given type [arguments].
     */
    fun type(vararg arguments: KSType, nullable: Boolean = false): KSType {
        val nonNull = ksType(declaration, *arguments)
        if (!nullable) return nonNull

        return ksType(declaration, *arguments, nullable = true).also { type ->
            every { type.makeNotNullable() } returns nonNull
        }
    }

    /**
     * Adds properties, after the class is created, for classes that reference each other.
     */
    fun extend(init: StructScope.() -> Unit): Struct = apply { StructScope(this).init() }

    internal fun add(property: KSPropertyDeclaration, parameter: KSValueParameter?) {
        properties.add(property)
        parameter?.let { parameters.add(it) }
    }
}

/**
 * Describes a class (struct is not a reserved word!) named [simpleName].
 *
 * @param simpleName The written name of the class.
 * @param qualifiedName The FQN. Prepends the package to [simpleName] by default.
 * @param packageName The package, by default the [qualifiedName] without its last part; set it for nested classes.
 * @param kind The kind of class, to describe declarations that can't be roots, such as interfaces.
 * @param annotations The annotations of the class, such as `@SchemaRoot`.
 * @param init A scope that allows to add properties to this struct.
 */
internal fun struct(
    simpleName: String,
    qualifiedName: String = "com.example.$simpleName",
    packageName: String = qualifiedName.substringBeforeLast('.', missingDelimiterValue = ""),
    kind: ClassKind = ClassKind.CLASS,
    annotations: List<KSAnnotation> = emptyList(),
    init: StructScope.() -> Unit = {},
): Struct = Struct(simpleName, qualifiedName, packageName, kind, annotations).extend(init)

/**
 * Declares the properties of a [Struct].
 */
@KsDsl
internal class StructScope(private val struct: Struct) {

    /**
     * Adds a property of the type that [type] returns.
     *
     * @param name The name.
     * @param hasDefault Whether its constructor parameter has a default.
     * @param inBody Whether it's declared in the class's body instead of the constructor.
     * @param hasBackingField Whether it holds data, unlike a computed property.
     * @param annotations The annotations.
     * @param type The type.
     */
    fun property(
        name: String,
        hasDefault: Boolean = false,
        inBody: Boolean = false,
        hasBackingField: Boolean = true,
        annotations: List<KSAnnotation> = emptyList(),
        type: TypeScope.() -> KSType,
    ): KSPropertyDeclaration = add(
        ksProperty(name, TypeScope(struct).type(), hasBackingField, annotations),
        name, hasDefault, inBody,
    )

    /**
     * Adds a property whose type is the class's type parameter at [typeArgument].
     *
     * @param name The name of the prop.
     * @param typeArgument The index in the class' type parameters that yields the type.
     * @param hasDefault Whether it has a default value or not. Matters in some dialects.
     * @param annotations The annotations.
     */
    fun property(
        name: String,
        typeArgument: Int,
        hasDefault: Boolean = false,
        annotations: List<KSAnnotation> = emptyList(),
    ): KSPropertyDeclaration {
        val property = ksProperty(name, annotations = annotations)
        every { property.asMemberOf(any()) } answers {
            firstArg<KSType>().arguments[typeArgument].type!!.resolve()
        }

        return add(property, name, hasDefault, inBody = false)
    }

    private fun add(
        property: KSPropertyDeclaration,
        name: String,
        hasDefault: Boolean,
        inBody: Boolean,
    ): KSPropertyDeclaration = property.also {
        struct.add(it, if (inBody) null else ksParameter(name, hasDefault))
    }
}

/**
 * Builds the type of property. Each function returns a type, so types nest as arguments:
 * `map(primitive(STRING), list(primitive(INTEGER)))`.
 *
 * @param struct The type scope to modify.
 */
@KsDsl
internal class TypeScope(private val struct: Struct) {

    /**
     * The Kotlin type a [PrimitiveType] is most commonly written as.
     *
     * @param type The type of primitive.
     * @param nullable Whether it is nullable or not.
     */
    fun primitive(type: PrimitiveType, nullable: Boolean = false): KSType = kotlin(
        when (type) {
            PrimitiveType.STRING -> "String"
            PrimitiveType.INTEGER -> "Int"
            PrimitiveType.NUMBER -> "Double"
            PrimitiveType.BOOLEAN -> "Boolean"
        }, nullable
    )

    /**
     * A type of the `kotlin` package, such as `UInt`, `IntArray` or `Any`.
     *
     * @param simpleName The name.
     * @param nullable Whether it is nullable or not.
     */
    fun kotlin(simpleName: String, nullable: Boolean = false): KSType =
        ksType("kotlin.$simpleName", nullable = nullable)

    /**
     * A collection of [element].
     *
     * @param element The type of element this collection type holds.
     * @param nullable Whether it is nullable or not.
     */
    fun list(element: KSType, nullable: Boolean = false): KSType =
        ksType("kotlin.collections.List", element, nullable = nullable)

    /**
     * A map from [key] to [value].
     *
     * @param key The type of element this map keys have.
     * @param value The type of element this map values holds.
     * @param nullable Whether it is nullable or not.
     */
    fun map(key: KSType, value: KSType, nullable: Boolean = false): KSType =
        ksType("kotlin.collections.Map", key, value, nullable = nullable)

    /**
     * An enum class with [entries].
     *
     * @param entries The options of the enum.
     * @param nullable Whether it is nullable or not.
     * @param qualifiedName The FQN.
     */
    fun enum(vararg entries: String, nullable: Boolean = false, qualifiedName: String = "com.example.Enum"): KSType =
        ksType(
            ksClassDeclaration(
                kind = ClassKind.ENUM_CLASS,
                qualifiedName = qualifiedName,
                declarations = entries.map { ksEntry(it) },
            ), nullable = nullable
        )

    /**
     * A type alias named [qualifiedName] for [target].
     *
     * @param qualifiedName The FQN.
     * @param target The type it points to.
     * @param nullable Whether nullable or otherwise.
     */
    fun alias(qualifiedName: String, target: KSType, nullable: Boolean = false): KSType =
        ksType(ksTypeAlias(qualifiedName, target), nullable = nullable)

    /**
     * A value class named [simpleName] that wraps [underlying].
     *
     * @param simpleName The name.
     * @param underlying The underlying type (what's really processed).
     * @param nullable Nullable or otherwise.
     */
    fun value(simpleName: String, underlying: KSType, nullable: Boolean = false): KSType {
        val declaration = ksClassDeclaration(
            modifiers = setOf(Modifier.VALUE),
            simpleName = simpleName,
            properties = listOf(ksProperty(type = underlying)),
        )
        val nonNull = ksType(declaration)
        if (!nullable) return nonNull

        return ksType(declaration, nullable = true).also { type -> every { type.makeNotNullable() } returns nonNull }
    }

    /**
     * A class used only here, described like [struct].
     *
     * @param simpleName The written name in code.
     * @param init A context to add properties and other things.
     */
    fun struct(simpleName: String, init: StructScope.() -> Unit = {}): KSType =
        Struct(simpleName, "com.example.$simpleName").extend(init).type()

    /**
     * The class being described, for classes that contain themselves.
     *
     * @param nullable Nullable or otherwise.
     */
    fun self(nullable: Boolean = false): KSType = struct.type(nullable = nullable)
}
