package com.vectornorthsystems.jsonschema.processor.writer

import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.ArrayNode
import com.vectornorthsystems.jsonschema.processor.node.MapNode
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.node.PrimitiveNode
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import com.vectornorthsystems.jsonschema.processor.node.RefNode
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * These tests compare the exact text written, so a change in the output of the schema (even in the order of the
 * keywords) is caught here.
 */
class SchemaWriterTest {

    private fun property(name: String, declaration: Node, required: Boolean = true) =
        PropertyNode(name, declaration, mockk(relaxed = true), required)

    private fun root(vararg properties: PropertyNode) =
        ObjectNode("Root", isRoot = true).apply { this.properties += properties }

    private fun string() = PrimitiveNode(PrimitiveType.STRING)

    private fun integer() = PrimitiveNode(PrimitiveType.INTEGER)

    // region Objects and primitives

    @Test
    fun `an object without properties has an empty properties object`() {
        // Given a root without properties:
        val root = root()

        // When writing...
        val schema = root.schema

        // We expect no required keyword:
        assertEquals("""{"type":"object","properties":{}}""", schema)
    }

    @Test
    fun `primitive properties are written with their type`() {
        // Given a root with one property of each primitive type:
        val root = root(
            property("a", PrimitiveNode(PrimitiveType.INTEGER)),
            property("b", PrimitiveNode(PrimitiveType.NUMBER)),
            property("c", PrimitiveNode(PrimitiveType.BOOLEAN)),
            property("d", PrimitiveNode(PrimitiveType.STRING)),
        )

        // When writing...
        val schema = root.schema

        // We expect each type, the properties in declaration order, and all of them required:
        assertEquals(
            """{"type":"object","properties":{""" +
                    """"a":{"type":"integer"},"b":{"type":"number"},"c":{"type":"boolean"},"d":{"type":"string"}},""" +
                    """"required":["a","b","c","d"]}""",
            schema
        )
    }

    @Test
    fun `only required properties are listed as required`() {
        // Given a root with an optional property between two required ones:
        val root = root(
            property("first", string()),
            property("optional", string(), required = false),
            property("last", string()),
        )

        // When writing...
        val schema = root.schema

        // We expect the optional property to be written, but not required:
        assertEquals(
            """{"type":"object","properties":{"first":{"type":"string"},"optional":{"type":"string"},""" +
                    """"last":{"type":"string"}},"required":["first","last"]}""",
            schema
        )
    }

    @Test
    fun `required is omitted when no property is required`() {
        // Given a root with only optional properties:
        val root = root(property("optional", string(), required = false))

        // When writing...
        val schema = root.schema

        // We expect no required keyword:
        assertEquals("""{"type":"object","properties":{"optional":{"type":"string"}}}""", schema)
    }

    @Test
    fun `a nullable type is written as a list of the type and null`() {
        // Given a nullable root with a nullable string and a nullable integer:
        val root = root(
            property("name", string().apply { nullable = true }),
            property("age", integer().apply { nullable = true }),
        ).apply { nullable = true }

        // When writing...
        val schema = root.schema

        // We expect "null" next to every type:
        assertEquals(
            """{"type":["object","null"],"properties":{"name":{"type":["string","null"]},""" +
                    """"age":{"type":["integer","null"]}},"required":["name","age"]}""",
            schema
        )
    }

    @Test
    fun `a nested object is written inline`() {
        // Given a root with a nested object, which has an optional property:
        val address = ObjectNode("Address").apply {
            properties += property("street", string())
            properties += property("floor", integer(), required = false)
        }
        val root = root(property("address", address))

        // When writing...
        val schema = root.schema

        // We expect the nested object in place, with its own required list:
        assertEquals(
            """{"type":"object","properties":{"address":{"type":"object","properties":""" +
                    """{"street":{"type":"string"},"floor":{"type":"integer"}},"required":["street"]}},""" +
                    """"required":["address"]}""",
            schema
        )
    }

    @Test
    fun `a nullable nested object has null in its type`() {
        // Given a root with a nullable nested object:
        val address = ObjectNode("Address").apply { nullable = true }
        val root = root(property("address", address))

        // When writing...
        val schema = root.schema

        // We expect the nested object to allow null:
        assertEquals(
            """{"type":"object","properties":{"address":{"type":["object","null"],"properties":{}}},""" +
                    """"required":["address"]}""",
            schema
        )
    }

    // endregion

    // region Descriptions

    @Test
    fun `title and description come before the type`() {
        // Given a property with a title and a description, and another with a description only:
        val root = root(
            property("name", string()).apply {
                title = "Name"
                description = "The name"
            },
            property("nick", string()).apply { description = "The nickname" },
        )

        // When writing...
        val schema = root.schema

        // We expect them in the property's schema, the title first:
        assertEquals(
            """{"type":"object","properties":{"name":{"title":"Name","description":"The name","type":"string"},""" +
                    """"nick":{"description":"The nickname","type":"string"}},"required":["name","nick"]}""",
            schema
        )
    }

    @Test
    fun `special characters are escaped`() {
        // Given a description with quotes, a backslash, and control characters, and an accented letter:
        val root = root(
            property("name", string()).apply {
                description = "a \"quote\", back\\slash, new\nline, tab\t, cr\r, bell\u0001, ff\u000c, bs\b, é"
            }
        )

        // When writing...
        val schema = root.schema

        // We expect the escapes of JSON, with the control characters in lowercase hex, and the letter untouched:
        assertEquals(
            """{"type":"object","properties":{"name":{"description":"a \"quote\", back\\slash, new\nline, """ +
                    """tab\t, cr\r, bell\u0001, ff\u000c, bs\b, é","type":"string"}},"required":["name"]}""",
            schema
        )
    }

    @Test
    fun `property names and enum values are escaped`() {
        // Given a property whose name has a quote, and an enum value with a backslash:
        val root = root(
            property("we\"ird", PrimitiveNode(PrimitiveType.STRING, enum = listOf("a\\b")))
        )

        // When writing...
        val schema = root.schema

        // We expect both to be escaped:
        assertEquals(
            """{"type":"object","properties":{"we\"ird":{"type":"string","enum":["a\\b"]}},"required":["we\"ird"]}""",
            schema
        )
    }

    // endregion

    // region Constraints

    @Test
    fun `an enum lists its values`() {
        // Given a string property that is an enum:
        val root = root(property("color", PrimitiveNode(PrimitiveType.STRING, enum = listOf("RED", "GREEN"))))

        // When writing...
        val schema = root.schema

        // We expect the values after the type:
        assertEquals(
            """{"type":"object","properties":{"color":{"type":"string","enum":["RED","GREEN"]}},""" +
                    """"required":["color"]}""",
            schema
        )
    }

    @Test
    fun `a format follows the type`() {
        // Given a string property with a format:
        val root = root(property("at", string().apply { format = "date-time" }))

        // When writing...
        val schema = root.schema

        // We expect the format after the type:
        assertEquals(
            """{"type":"object","properties":{"at":{"type":"string","format":"date-time"}},"required":["at"]}""",
            schema
        )
    }

    @Test
    fun `whole limits are written without a fraction`() {
        // Given an integer property with whole limits, and a number property with a negative whole minimum:
        val root = root(
            property("count", integer().apply {
                minimum = 1.0
                maximum = 10.0
            }),
            property("delta", PrimitiveNode(PrimitiveType.NUMBER).apply { minimum = -5.0 }),
        )

        // When writing...
        val schema = root.schema

        // We expect 1 and 10 instead of 1.0 and 10.0:
        assertEquals(
            """{"type":"object","properties":{"count":{"type":"integer","minimum":1,"maximum":10},""" +
                    """"delta":{"type":"number","minimum":-5}},"required":["count","delta"]}""",
            schema
        )
    }

    @Test
    fun `fractional limits keep their fraction`() {
        // Given a number property with fractional limits:
        val root = root(
            property("ratio", PrimitiveNode(PrimitiveType.NUMBER).apply {
                minimum = 0.5
                maximum = 99.99
            })
        )

        // When writing...
        val schema = root.schema

        // We expect the fractions to be kept:
        assertEquals(
            """{"type":"object","properties":{"ratio":{"type":"number","minimum":0.5,"maximum":99.99}},""" +
                    """"required":["ratio"]}""",
            schema
        )
    }

    @Test
    fun `a limit as large as 1e15 is written in scientific notation`() {
        // Given an integer property with a minimum of 10^15, the first value that isn't written as a whole number:
        val root = root(property("big", integer().apply { minimum = 1e15 }))

        // When writing...
        val schema = root.schema

        // We expect the notation of Double.toString:
        assertEquals(
            """{"type":"object","properties":{"big":{"type":"integer","minimum":1.0E15}},"required":["big"]}""",
            schema
        )
    }

    // endregion

    // region Arrays and maps

    @Test
    fun `an array describes its items`() {
        // Given a nullable array of strings:
        val root = root(property("tags", ArrayNode(string()).apply { nullable = true }))

        // When writing...
        val schema = root.schema

        // We expect the type of the array with null, and the item's schema:
        assertEquals(
            """{"type":"object","properties":{"tags":{"type":["array","null"],"items":{"type":"string"}}},""" +
                    """"required":["tags"]}""",
            schema
        )
    }

    @Test
    fun `item limits come after the items`() {
        // Given an array with a minimum and a maximum number of items:
        val root = root(property("tags", ArrayNode(string()).apply {
            minItems = 1
            maxItems = 3
        }))

        // When writing...
        val schema = root.schema

        // We expect the limits after the items:
        assertEquals(
            """{"type":"object","properties":{"tags":{"type":"array","items":{"type":"string"},""" +
                    """"minItems":1,"maxItems":3}},"required":["tags"]}""",
            schema
        )
    }

    @Test
    fun `prefix items come before the items`() {
        // Given an array with a string and an integer as its first items:
        val root = root(property("tuple", ArrayNode(string()).apply {
            prefixItems = listOf(string(), integer())
        }))

        // When writing...
        val schema = root.schema

        // We expect the prefix items before the items:
        assertEquals(
            """{"type":"object","properties":{"tuple":{"type":"array","prefixItems":""" +
                    """[{"type":"string"},{"type":"integer"}],"items":{"type":"string"}}},"required":["tuple"]}""",
            schema
        )
    }

    @Test
    fun `an array of arrays nests the items`() {
        // Given an array of arrays of integers:
        val root = root(property("grid", ArrayNode(ArrayNode(integer()))))

        // When writing...
        val schema = root.schema

        // We expect the items to be nested:
        assertEquals(
            """{"type":"object","properties":{"grid":{"type":"array","items":{"type":"array","items":""" +
                    """{"type":"integer"}}}},"required":["grid"]}""",
            schema
        )
    }

    @Test
    fun `a map describes its values as additional properties`() {
        // Given a nullable map of strings to integers, and a map of arrays:
        val root = root(
            property("scores", MapNode(integer()).apply { nullable = true }),
            property("groups", MapNode(ArrayNode(string()))),
        )

        // When writing...
        val schema = root.schema

        // We expect an object with the values' schema as additionalProperties:
        assertEquals(
            """{"type":"object","properties":{"scores":{"type":["object","null"],"additionalProperties":""" +
                    """{"type":"integer"}},"groups":{"type":"object","additionalProperties":{"type":"array",""" +
                    """"items":{"type":"string"}}}},"required":["scores","groups"]}""",
            schema
        )
    }

    // endregion

    // region References and definitions

    @Test
    fun `a reference to the root points to the document`() {
        // Given a root with a list of itself, and a nullable reference to itself:
        val root = ObjectNode("Root", isRoot = true)
        root.properties += property("children", ArrayNode(RefNode(root)))
        root.properties += property("parent", RefNode(root).apply { nullable = true }, required = false)

        // When writing...
        val schema = root.schema

        // We expect # for the root, without definitions, and anyOf for the nullable reference:
        assertEquals(
            $$"""{"type":"object","properties":{"children":{"type":"array","items":{"$ref":"#"}},""" +
                    $$""""parent":{"anyOf":[{"$ref":"#"},{"type":"null"}]}},"required":["children"]}""",
            schema
        )
    }

    @Test
    fun `a root that is referenced is still written in place`() {
        // Given a root marked as referenced:
        val root = ObjectNode("Root", isRoot = true).apply { referenced = true }

        // When writing...
        val schema = root.schema

        // We expect the object, not a reference to it:
        assertEquals("""{"type":"object","properties":{}}""", schema)
    }

    @Test
    fun `a recursive object is defined once and referenced`() {
        // Given a root with a node that points to itself:
        val node = ObjectNode("Node").apply { referenced = true }
        node.properties += property("next", RefNode(node), required = false)
        val root = root(property("head", node))

        // When writing...
        val schema = root.schema

        // We expect both the first occurrence and the reference to point to the definition:
        assertEquals(
            $$"""{"type":"object","properties":{"head":{"$ref":"#/$defs/Node"}},"required":["head"],""" +
                    $$""""$defs":{"Node":{"type":"object","properties":{"next":{"$ref":"#/$defs/Node"}}}}}""",
            schema
        )
    }

    @Test
    fun `a definition is written once however often it is referenced`() {
        // Given a root that refers to the same recursive object in two properties:
        val node = ObjectNode("Node").apply { referenced = true }
        node.properties += property("next", RefNode(node), required = false)
        val root = root(property("first", node), property("second", RefNode(node)))

        // When writing...
        val schema = root.schema

        // We expect a single definition:
        assertEquals(
            $$"""{"type":"object","properties":{"first":{"$ref":"#/$defs/Node"},"second":{"$ref":"#/$defs/Node"}},""" +
                    $$""""required":["first","second"],"$defs":{"Node":{"type":"object","properties":""" +
                    $$"""{"next":{"$ref":"#/$defs/Node"}}}}}""",
            schema
        )
    }

    @Test
    fun `a nullable reference allows null but its definition does not`() {
        // Given a recursive object that is nullable:
        val node = ObjectNode("Node").apply {
            referenced = true
            nullable = true
        }
        node.properties += property("next", RefNode(node).apply { nullable = true }, required = false)
        val root = root(property("head", node))

        // When writing...
        val schema = root.schema

        // We expect anyOf with null where it's referenced, and a plain object as the definition:
        assertEquals(
            $$"""{"type":"object","properties":{"head":{"anyOf":[{"$ref":"#/$defs/Node"},{"type":"null"}]}},""" +
                    $$""""required":["head"],"$defs":{"Node":{"type":"object","properties":""" +
                    $$"""{"next":{"anyOf":[{"$ref":"#/$defs/Node"},{"type":"null"}]}}}}}""",
            schema
        )
    }

    @Test
    fun `definitions found while writing definitions are appended in the order they are met`() {
        // Given a root that refers to a recursive object A, which refers to a recursive object B:
        val b = ObjectNode("B").apply { referenced = true }
        b.properties += property("self", RefNode(b), required = false)
        val a = ObjectNode("A").apply { referenced = true }
        a.properties += property("b", b, required = false)
        a.properties += property("self", RefNode(a), required = false)
        val root = root(property("a", a))

        // When writing...
        val schema = root.schema

        // We expect A defined first, then B:
        assertEquals(
            $$"""{"type":"object","properties":{"a":{"$ref":"#/$defs/A"}},"required":["a"],"$defs":{""" +
                    $$""""A":{"type":"object","properties":{"b":{"$ref":"#/$defs/B"},"self":{"$ref":"#/$defs/A"}}},""" +
                    $$""""B":{"type":"object","properties":{"self":{"$ref":"#/$defs/B"}}}}}""",
            schema
        )
    }

    @Test
    fun `a definition is named after its unique definition name`() {
        // Given a recursive object whose definition name differs from its class name, as when two classes share it:
        val node = ObjectNode("Node").apply {
            referenced = true
            defName = "Node2"
        }
        node.properties += property("next", RefNode(node), required = false)
        val root = root(property("head", node))

        // When writing...
        val schema = root.schema

        // We expect the definition name in the reference and as the key:
        assertEquals(
            $$"""{"type":"object","properties":{"head":{"$ref":"#/$defs/Node2"}},"required":["head"],""" +
                    $$""""$defs":{"Node2":{"type":"object","properties":{"next":{"$ref":"#/$defs/Node2"}}}}}""",
            schema
        )
    }

    @Test
    fun `a reference property keeps its title and description`() {
        // Given a property with a title that refers to a recursive object:
        val node = ObjectNode("Node").apply { referenced = true }
        val root = root(property("head", node).apply { title = "Head" })

        // When writing...
        val schema = root.schema

        // We expect the title next to the reference:
        assertEquals(
            $$"""{"type":"object","properties":{"head":{"title":"Head","$ref":"#/$defs/Node"}},""" +
                    $$""""required":["head"],"$defs":{"Node":{"type":"object","properties":{}}}}""",
            schema
        )
    }

    // endregion
}
