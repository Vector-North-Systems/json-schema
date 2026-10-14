package com.vectornorthsystems.jsonschema.processor

/**
 * Returns the qualified name of a type.
 * @param T the type whose qualified name is requested.
 */
inline fun <reified T> getQualifiedName(): String {
    return "${T::class.qualifiedName}"
}

private val identifier = Regex("[\\p{L}_][\\p{L}\\p{N}_]*")

/**
 * Kotlin's hard keywords, which can't be identifiers without backticks.
 */
private val hardKeywords = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
    "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var",
    "when", "while",
)

/**
 * Tells whether the string can be used as a Kotlin identifier without backticks.
 */
internal fun String.isIdentifier(): Boolean =
    matches(identifier) && this !in hardKeywords && any { it != '_' } // Only underscores is reserved.
