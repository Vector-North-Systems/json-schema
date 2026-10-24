package com.vectornorthsystems.jsonschema

import kotlin.reflect.KClass

/**
 * Defines the entry point and root of a schema.
 * All the properties of the annotated class, and the properties of its respective types, will be processed
 * at compile time, and included in the result schema.
 * The result schema can be retrieved by calling `get<ClassName>Schema()`, which returns a [String]
 * and is generated in the annotated class's package.
 *
 * @property functionName The name of the generated function, instead of `get<ClassName>Schema`. It must be a valid
 * Kotlin identifier, and unique in the package: two nested classes with the same name need different function names.
 */
@Target(AnnotationTarget.CLASS)
annotation class SchemaRoot(val functionName: String = "")

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
 * The types of the first items of an array property, for tuple-like structures. Items after these follow the array's
 * element type.
 *
 * Deprecated: models follow `prefixItems` and produce arrays of mixed types, which a Kotlin collection can't hold, so
 * the response fails to decode. With matching types, it adds nothing to the element type. Use a data class instead:
 * its properties decode, and their names tell the model what each value means.
 */
@Deprecated(
    message = "Responses with mixed-type arrays can't be decoded into a Kotlin collection. " +
            "Use a data class instead. Will be removed before 1.0.",
)
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaPrefixItems(vararg val types: KClass<*>)

/**
 * Excludes a property from the generated schema.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class SchemaExclude
