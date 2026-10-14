package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaExclude
import com.vectornorthsystems.jsonschema.SchemaRoot

open class Entity(val id: String)

/**
 * Computed and delegated properties aren't data, so they're left out.
 * Inherits `id` from [Entity], and it's optional because the constructor of [Account] gives it a default.
 */
@SchemaRoot
class Account(id: String = "", val email: String = "") : Entity(id) {
    // Left out since is computed
    val domain: String
        get() = email.substringAfter("@")

    // Left out since delegated
    val normalized: String by lazy { email.lowercase() }
}

/**
 * Required-ness follows the constructor of the class described: `id` is passed on to [Entity] without a default, so it's
 * required, while `joined` is declared in the body, so it's optional.
 */
@SchemaRoot
class Member(id: String) : Entity(id) {
    var joined: Long = 0
}

/**
 * Properties marked with [SchemaExclude] are left out, whether declared in the constructor or the body.
 */
@SchemaRoot
data class Session(
    val user: String = "",
    @SchemaExclude
    val token: String = "",
) {
    @SchemaExclude
    var lastSeen: Long = 0
}
