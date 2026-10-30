package com.vectornorthsystems.fixtures

import com.vectornorthsystems.assertSchemas
import kotlin.test.Test

/**
 * Object structure and required properties.
 */
class ObjectSchemaTest {

    @Test
    fun `complex is simple`() {
        // Two numbers, neither required since both have defaults.
        assertSchemas("Complex", ComplexSchema::getOrNull)
    }

    @Test
    fun `objects, arrays, enums and parameters without a default are required`() {
        // `name` and `ingredients` are required, and so are an ingredient's `name` and `quantity`, and a quantity's
        // `amount`; `unitSystem` lists its entries.
        assertSchemas("Recipe", RecipeSchema::getOrNull)
    }

    @Test
    fun `inherited properties are included, computed and delegated ones are not`() {
        // Only `email` and the inherited `id`.
        assertSchemas("Account", AccountSchema::getOrNull)
    }

    @Test
    fun `constructor parameters without a default are required, body properties are optional`() {
        // `id` is required, the body property `joined` isn't.
        assertSchemas("Member", MemberSchema::getOrNull)
    }

    @Test
    fun `body properties must be initialized, so they are optional`() {
        // `length` is required, the body property `mass` isn't.
        assertSchemas("BodyProperties", BodyPropertiesSchema::getOrNull)
    }

    @Test
    fun `excluded properties are left out`() {
        // Only `user`.
        assertSchemas("Session", SessionSchema::getOrNull)
    }
}
