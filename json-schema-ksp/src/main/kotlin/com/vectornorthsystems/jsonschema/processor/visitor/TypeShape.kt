package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.vectornorthsystems.jsonschema.processor.PrimitiveType

/**
 * What kind of schema a Kotlin type produces, as decided by [TypeClassifier].
 */
internal sealed interface TypeShape {
    /**
     * A type alias, described by the type it stands for.
     */
    data class Alias(val target: KSType) : TypeShape

    /**
     *  A number, boolean or string, with the minimum unsigned types start with.
     */
    data class Primitive(val type: PrimitiveType, val minimum: Double? = null) : TypeShape

    /**
     * A primitive array such as [IntArray], by the qualified name of the array type.
     */
    data class PrimitiveCollection(val qualifiedName: String) : TypeShape

    /**
     * An array or collection of [element].
     */
    data class Collection(val element: KSType) : TypeShape

    /**
     * A map from [key] to [value].
     */
    data class MapOf(val key: KSType, val value: KSType) : TypeShape

    /**
     * An enum class with its entry names.
     */
    data class EnumOf(val entries: List<String>) : TypeShape

    /**
     * A value class, which is serialized as the [underlying] type it wraps.
     */
    data class Value(val underlying: KSType) : TypeShape

    /**
     * A class described by its properties.
     */
    data class Object(val declaration: KSClassDeclaration) : TypeShape

    /**
     * A type that can't be described, and why.
     */
    data class Unsupported(val reason: String) : TypeShape
}
