package com.vectornorthsystems.fixtures

import com.vectornorthsystems.assertSchemas
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Recursion, shared classes and generics, which are defined once in `$defs`.
 */
class ReferenceSchemaTest {

    @Test
    fun `a root that contains itself is referenced with #`() {
        // `children` items are `{"$ref": "#"}`, with no `$defs`.
        assertSchemas("TreeNode", TreeNodeSchema::getOrNull)
    }

    @Test
    fun `a nested class that contains itself is defined once in defs`() {
        // `Employee` is in `$defs` once, and referenced from `head` and from itself.
        assertSchemas("Organization", OrganizationSchema::getOrNull)
    }

    @Test
    fun `the same nested class should exist only once`() {
        // `ceo` and `cto` reference a single `Employee` in `$defs`.
        assertSchemas("Org", OrgSchema::getOrNull)
    }

    @Test
    fun `a non-recursive class used twice is defined once and referenced twice`() {
        // `billing` and `shipping` reference a single `Address` in `$defs`.
        assertSchemas("Customer", CustomerSchema::getOrNull)
    }

    @Test
    fun `more than 32 sibling classes do not hit the depth limit`() {
        val schema = Json.parseToJsonElement(WideSchema.GEMINI).jsonObject
        assertEquals(40, schema.getValue("properties").jsonObject.size)
        assertNull(schema[$$"$defs"])
    }

    @Test
    fun `type parameters take the type arguments of each use`() {
        // Each `Box` is inlined with the type of its use: a string, an integer, and a list of boxes of booleans.
        assertSchemas("Shelf", ShelfSchema::getOrNull)
    }
}
