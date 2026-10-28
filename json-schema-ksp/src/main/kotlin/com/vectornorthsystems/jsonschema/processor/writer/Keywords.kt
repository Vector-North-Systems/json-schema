package com.vectornorthsystems.jsonschema.processor.writer

/**
 * The supported keywords (subset of JSON Schema).
 */
internal object Types {
    /**
     * Text.
     */
    const val STRING = "string"

    /**
     * Floating point numbers.
     */
    const val NUMBER = "number"

    /**
     * Whole numbers.
     */
    const val INTEGER = "integer"

    /**
     * True or false.
     */
    const val BOOLEAN = "boolean"

    /**
     * Structured data with key-value pairs.
     */
    const val OBJECT = "object"

    /**
     * Lists of items.
     */
    const val ARRAY = "array"

    /**
     * The absence of a value.
     */
    const val NULL = "null"
}

/**
 * Descriptive properties
 */
internal object Description {
    /**
     * A short description of a property.
     */
    const val TITLE = "title"

    /**
     * A longer and more detailed description of a property.
     */
    const val DESCRIPTION = "description"
}

/**
 * Structural keywords, which describe and connect schemas.
 */
internal object Structure {
    /**
     * The type of value, one of [Types], or an array of them to allow several.
     */
    const val TYPE = "type"

    /**
     * A reference to a schema defined elsewhere, e.g. in [DEFS].
     */
    const val REF = $$"$ref"

    /**
     * Schemas defined once and referenced with [REF].
     */
    const val DEFS = $$"$defs"

    /**
     * The value is valid against at least one of a list of schemas.
     */
    const val ANY_OF = "anyOf"
}

// Type-specific properties:

/**
 * Properties for [Types.OBJECT] values.
 */
internal object ObjectProperties {
    /**
     * An object where each key is a property name and each value is a schema for that property.
     */
    const val PROPERTIES = "properties"

    /**
     * An array of strings, listing which properties are mandatory.
     */
    const val REQUIRED = "required"

    /**
     * Controls whether properties not listed in [PROPERTIES] are allowed. Can be a boolean or a schema.
     */
    const val ADDITIONAL_PROPERTIES = "additionalProperties"
}

/**
 * Properties for [Types.STRING] values.
 */
internal object StringProperties {
    /**
     * Lists a specific set of possible strings for classification tasks.
     */
    const val ENUM = "enum"

    /**
     * Specifies a syntax for the string. See [Formats].
     */
    const val FORMAT = "format"
}

/**
 * Supported values for [StringProperties.FORMAT].
 * Present here for completeness, but used from the annotations.
 */
internal object Formats {
    const val DATE_TIME = "date-time"
    const val DATE = "date"
    const val TIME = "time"
}

/**
 * Properties for [Types.NUMBER] and [Types.INTEGER] values.
 */
internal object NumberProperties {
    /**
     * Lists a specific set of possible numeric values.
     */
    const val ENUM = "enum"

    /**
     * The minimum inclusive value.
     */
    const val MINIMUM = "minimum"

    /**
     * The maximum inclusive value.
     */
    const val MAXIMUM = "maximum"
}

/**
 * Properties for [Types.ARRAY] values.
 */
internal object ArrayProperties {
    /**
     * Defines the schema for all items in the array.
     */
    const val ITEMS = "items"

    /**
     * The minimum number of items in the array.
     */
    const val MIN_ITEMS = "minItems"

    /**
     * The maximum number of items in the array.
     */
    const val MAX_ITEMS = "maxItems"
}
