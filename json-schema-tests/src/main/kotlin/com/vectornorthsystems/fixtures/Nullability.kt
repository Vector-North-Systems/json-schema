package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRequired
import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * Typealias, resolved to the underlying type (String in this case).
 */
typealias Nickname = String?

/**
 * Nullable types allow `null`, which is independent of a property being required: parameters without a default are
 * required, and so are the ones marked with [SchemaRequired].
 */
@SchemaRoot
data class Profile(
    val name: String,
    val bio: String?,
    val age: Int? = null,
    val tags: List<String?>? = null,
    val unitSystem: UnitSystem? = null,
    val quantity: Quantity? = null,
    val nickname: Nickname = null,
    @SchemaRequired
    val website: String? = null,
)

/**
 * Nullable enums list `null` among their values, whether they're required, list items or map values. The same enum
 * used without `?`, even inside a nullable collection, doesn't.
 */
@SchemaRoot
data class UnitPreferences(
    val system: UnitSystem?,
    val fallback: UnitSystem,
    val history: List<UnitSystem?>,
    val bySite: Map<String, UnitSystem?>,
    val preferred: List<UnitSystem>? = null,
)
