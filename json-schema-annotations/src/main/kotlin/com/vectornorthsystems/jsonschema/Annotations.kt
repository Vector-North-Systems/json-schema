package com.vectornorthsystems.jsonschema

/**
 * Defines the entry point and root of a schema.
 *
 * All the properties of the annotated class, and the properties of its respective types, will be processed
 * at compile time, and included in the resulting schema.
 *
 * The resulting schemas can be retrieved from the properties of a `{ClassName}Schema` object generated in the
 * annotated class's package, where `ClassName` is the name of the annotated class. The object has one [String]
 * property per target dialect, named after it, holding the schema in that dialect (see [SchemaTarget] for the
 * supported dialects). For example, the class:
 *
 * ```kotlin
 * @SchemaRoot
 * data class Star(val type: String)
 * ```
 *
 * Generates:
 * ```kotlin
 * object StarSchema {
 *     const val GEMINI = """{"type":"object",...}"""
 *     // ... one property per target dialect.
 *
 *     fun getOrNull(target: SchemaTarget): String? = ...
 *     operator fun get(target: SchemaTarget): String = ...
 * }
 * ```
 *
 * Use the property of the dialect you need, `StarSchema.GEMINI`, or pick one at runtime: `StarSchema[target]` throws an
 * [IllegalArgumentException] for a dialect that isn't generated for the class, and `StarSchema.getOrNull(target)`
 * returns `null` instead.
 *
 * By default, every dialect in [SchemaTarget] is generated. A module can narrow them down with a KSP argument, a
 * comma separated list of dialects, in any case:
 * ```kotlin
 * // build.gradle.kts
 * ksp { arg("jsonSchema.targets", "gemini") }
 * ```
 *
 * The code above generates only the `GEMINI` property, for all the [SchemaRoot] annotated classes in the module. An
 * unknown or empty list fails the build.
 *
 * A class can override the module's dialects with [targets]:
 *
 * ```kotlin
 * // Generate only the GEMINI property for this class:
 * @SchemaRoot(targets = [SchemaTarget.GEMINI])
 * data class Star(val type: String)
 * ```
 *
 * The code above generates only:
 * ```kotlin
 * object StarSchema {
 *     const val GEMINI = """{"type":"object",...}"""
 * }
 * ```
 *
 * The name of the object can also be overridden:
 *
 * ```kotlin
 * // Override the name and targets:
 * @SchemaRoot(name = "StarCraft", targets = [SchemaTarget.GEMINI])
 * data class Star(val type: String)
 * ```
 *
 * The code above generates:
 * ```kotlin
 * object StarCraft {
 *     const val GEMINI = """{"type":"object",...}"""
 * }
 * ```
 *
 * @property name The name of the generated object, instead of `{ClassName}Schema`. It must be a valid Kotlin
 * identifier, and unique in the package: two nested classes with the same name need different names.
 * @property targets The target dialects to generate for this class. When empty, the module's dialects are generated:
 * all of them, or those set by the `jsonSchema.targets` KSP argument.
 */
@Target(AnnotationTarget.CLASS)
annotation class SchemaRoot(
    val name: String = "",
    val targets: Array<SchemaTarget> = []
)

/**
 * A short description of the property.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaTitle(val text: String)

/**
 * Describes a property in the generated response schema.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaDescription(val text: String)

/**
 * Marks a property the AI model must include, even though it has a default.
 * Primary constructor parameters without a default are already required, since a decoder can't build the object
 * without them. Properties with a default, or declared in the class body, are optional unless annotated with this.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaRequired

/**
 * Defines date and time formats on a [String] property.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaFormat(val format: Format) {
    enum class Format(val value: String) {
        DATE_TIME("date-time"),
        DATE("date"),
        TIME("time")
    }
}

/**
 * The minimum inclusive value of a number property.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaMinimum(val value: Double)

/**
 * The maximum inclusive value of a number property.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaMaximum(val value: Double)

/**
 * The minimum number of items in an array property.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaMinItems(val value: Int)

/**
 * The maximum number of items in an array property.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaMaxItems(val value: Int)

/**
 * Excludes a property from the generated schema.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaExclude
