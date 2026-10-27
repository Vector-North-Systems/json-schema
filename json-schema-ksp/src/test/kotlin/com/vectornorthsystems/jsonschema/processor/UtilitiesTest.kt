package com.vectornorthsystems.jsonschema.processor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilitiesTest {

    // region isIdentifier

    @Test
    fun isIdentifier() {
        // Given names with letters, digits, underscores, accents, other scripts, and soft keywords:
        val names = listOf("name", "Name", "_name", "name_2", "a1", "naïve", "名前", "value", "data", "open")

        names.forEach {
            // When checking them...
            val identifier = it.isIdentifier()

            // We expect them to be valid identifiers:
            assertTrue(identifier, "\"$it\" should be an identifier")
        }
    }

    @Test
    fun isNotIdentifier() {
        // Given names that are empty, start with a digit, or have dashes, spaces, symbols, backticks or dots:
        val names = listOf("", "1a", "my-name", "has space", "dollar\$", "`quoted`", "a.b")

        names.forEach {
            // When checking them...
            val identifier = it.isIdentifier()

            // We expect them not to be identifiers:
            assertFalse(identifier, "\"$it\" should not be an identifier")
        }
    }

    @Test
    fun hardKeywordsAreNotIdentifiers() {
        // Given Kotlin's hard keywords:
        val keywords = listOf("class", "val", "var", "fun", "object", "null", "true", "when", "typeof")

        keywords.forEach {
            // When checking them...
            val identifier = it.isIdentifier()

            // We expect them not to be identifiers:
            assertFalse(identifier, "\"$it\" is a hard keyword")
        }
    }

    @Test
    fun onlyUnderscoresAreNotIdentifiers() {
        // Given names made only of underscores:
        val names = listOf("_", "__", "___")

        names.forEach {
            // When checking them...
            val identifier = it.isIdentifier()

            // We expect them not to be identifiers, since they're reserved:
            assertFalse(identifier, "\"$it\" is reserved")
        }
    }

    // endregion
}
