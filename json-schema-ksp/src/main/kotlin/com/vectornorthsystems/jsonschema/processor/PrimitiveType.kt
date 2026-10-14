package com.vectornorthsystems.jsonschema.processor

import com.vectornorthsystems.jsonschema.processor.writer.Types

/**
 * The primitive types.
 */
internal enum class PrimitiveType(val keyword: String) {
    INTEGER(Types.INTEGER),
    NUMBER(Types.NUMBER),
    BOOLEAN(Types.BOOLEAN),
    STRING(Types.STRING)
}