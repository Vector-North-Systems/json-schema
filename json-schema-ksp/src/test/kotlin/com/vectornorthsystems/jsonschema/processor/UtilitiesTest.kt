package com.vectornorthsystems.jsonschema.processor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilitiesTest {
    @Test
    fun isIdentifier() {
        listOf("name", "Name", "_name", "name_2", "a1", "naïve", "名前", "value", "data", "open").forEach {
            assertTrue(it.isIdentifier(), "\"$it\" should be an identifier")
        }
    }

    @Test
    fun isNotIdentifier() {
        listOf("", "1a", "my-name", "has space", "dollar\$", "`quoted`", "a.b").forEach {
            assertFalse(it.isIdentifier(), "\"$it\" should not be an identifier")
        }
    }

    @Test
    fun hardKeywordsAreNotIdentifiers() {
        listOf("class", "val", "var", "fun", "object", "null", "true", "when", "typeof").forEach {
            assertFalse(it.isIdentifier(), "\"$it\" is a hard keyword")
        }
    }

    @Test
    fun onlyUnderscoresAreNotIdentifiers() {
        listOf("_", "__", "___").forEach {
            assertFalse(it.isIdentifier(), "\"$it\" is reserved")
        }
    }
}
