package com.vectornorthsystems.jsonschema.processor.visitor

import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.getQualifiedName

/**
 * Integer types.
 * Namely: `kotlin.Int`, `kotlin.Long`, `kotlin.Short` and `kotlin.Byte`.
 */
internal val knownIntegerTypes = listOf(
    getQualifiedName<Int>(),
    getQualifiedName<Long>(),
    getQualifiedName<Short>(),
    getQualifiedName<Byte>(),
)

/**
 * Unsigned types (Map to integer as well).
 * Namely: `UInt`, `ULong`, `UShort` and `UByte`.
 */
internal val knownUnsignedTypes = listOf(
    getQualifiedName<UInt>(),
    getQualifiedName<ULong>(),
    getQualifiedName<UShort>(),
    getQualifiedName<UByte>()
)

/**
 * Number (floating point) types.
 * Namely: `Float` and `Double`.
 */
internal val knownNumberTypes = listOf(
    getQualifiedName<Float>(),
    getQualifiedName<Double>()
)

/**
 * Arrays of primitives, which have no type argument for their element type.
 */
internal val knownPrimitiveArrayTypes = mapOf(
    getQualifiedName<IntArray>() to PrimitiveType.INTEGER,
    getQualifiedName<LongArray>() to PrimitiveType.INTEGER,
    getQualifiedName<ShortArray>() to PrimitiveType.INTEGER,
    getQualifiedName<ByteArray>() to PrimitiveType.INTEGER,
    getQualifiedName<FloatArray>() to PrimitiveType.NUMBER,
    getQualifiedName<DoubleArray>() to PrimitiveType.NUMBER,
    getQualifiedName<BooleanArray>() to PrimitiveType.BOOLEAN,
    getQualifiedName<CharArray>() to PrimitiveType.STRING
)

/**
 * Collection classes that produce "array".
 * The mutable ones are written out because on the JVM they share a class with their read-only counterpart,
 * e.g. `MutableList::class.qualifiedName` is `kotlin.collections.List`.
 */
internal val knownArrayTypes = listOf(
    getQualifiedName<Array<*>>(),
    getQualifiedName<List<*>>(),
    getQualifiedName<Set<*>>(),
    getQualifiedName<Collection<*>>(),
    getQualifiedName<Iterable<*>>(),
    "kotlin.collections.MutableList",
    "kotlin.collections.MutableSet",
    "kotlin.collections.MutableCollection",
    "kotlin.collections.MutableIterable"
)

/**
 * Boolean.
 */
internal val knownBooleanTypes = listOf(getQualifiedName<Boolean>())

/**
 * Strings.
 */
internal val knownStringTypes = listOf(
    getQualifiedName<String>(),
    getQualifiedName<Char>(),
)

/**
 * Maps, which produce "object" with additionalProperties.
 * `MutableMap` is written out for the same reason as in [knownArrayTypes].
 */
internal val knownMapTypes = listOf(
    getQualifiedName<Map<*, *>>(),
    "kotlin.collections.MutableMap"
)