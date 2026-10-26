package com.vectornorthsystems

import com.vectornorthsystems.fixtures.getDollarsSchema
import com.vectornorthsystems.fixtures.getEscapingSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class EscapingTest {
    @Test
    fun `annotation text survives Kotlin generation and JSON parsing`() {
        val property = Json.parseToJsonElement(getEscapingSchema()).jsonObject
            .getValue("properties").jsonObject.getValue("value").jsonObject
        assertEquals((0..31).map { it.toChar() }.joinToString(""), property.getValue("title").jsonPrimitive.content)
        assertEquals(
            "Quotes: \"\"\"; slash: \\; Unicode: café 雪 😀; template: \$name \${value}",
            property.getValue("description").jsonPrimitive.content
        )
    }

    @Test
    fun `runs of dollar signs stay literal`() {
        val property = Json.parseToJsonElement(getDollarsSchema()).jsonObject
            .getValue("properties").jsonObject.getValue("value").jsonObject
        assertEquals(
            "price: \$\${amount}, \$\$\$, \${name} and \$name",
            property.getValue("description").jsonPrimitive.content
        )
    }
}
