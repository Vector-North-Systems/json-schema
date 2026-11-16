package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.mockk
import kotlin.test.*

class RequireAllPropertiesTest {

    private fun property(name: String, declaration: Node, required: Boolean = false) =
        PropertyNode(name = name, declaration = declaration, source = mockk<KSNode>(), required = required)

    private fun string() = PrimitiveNode(PrimitiveType.STRING)

    private fun ObjectNode.property(name: String) = properties.single { it.name == name }

    @Test
    fun `optional properties become required, and required ones stay required`() {
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("optional", string())
            properties += property("required", string(), required = true)
        }

        val result = RequireAllProperties().transform(root) as ObjectNode

        assertTrue(result.property("optional").required)
        assertTrue(result.property("required").required)
    }

    @Test
    fun `properties are required at every depth`() {
        // Root.child: Child, Root.items: List<Item>, Root.byKey: Map<String, Value>, Child.self: Child.
        val child = ObjectNode("Child").apply { properties += property("name", string()) }
        child.properties += property("self", RefNode(child))
        val item = ObjectNode("Item").apply { properties += property("name", string()) }
        val value = ObjectNode("Value").apply { properties += property("name", string()) }
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("child", child)
            properties += property("items", ArrayNode(item))
            properties += property("byKey", MapNode(value, mockk()))
        }

        val result = RequireAllProperties().transform(root) as ObjectNode

        val resultChild = result.property("child").declaration as ObjectNode
        val resultItem = (result.property("items").declaration as ArrayNode).declaration as ObjectNode
        val resultValue = (result.property("byKey").declaration as MapNode).declaration as ObjectNode
        val all = result.properties + resultChild.properties + resultItem.properties + resultValue.properties
        assertEquals(7, all.size)
        all.forEach { assertTrue(it.required, "${it.name} should be required") }

        // The reference points at the transformed object, whose properties are required.
        val target = (resultChild.property("self").declaration as RefNode).target
        assertSame(resultChild, target)
    }

    @Test
    fun `the rest of the property is copied unchanged`() {
        val source = mockk<KSNode>()
        val declaration = PrimitiveNode(PrimitiveType.INTEGER).apply { nullable = true; minimum = 1.0 }
        val original = PropertyNode("count", declaration, source, required = false).apply {
            title = "Count"
            description = "How many"
            nullable = true
        }
        val root = ObjectNode("Root", isRoot = true).apply { properties += original }

        val result = (RequireAllProperties().transform(root) as ObjectNode).property("count")

        assertEquals("count", result.name)
        assertSame(source, result.source)
        assertEquals("Count", result.title)
        assertEquals("How many", result.description)
        assertTrue(result.nullable)
        val resultDeclaration = assertIs<PrimitiveNode>(result.declaration)
        assertEquals(PrimitiveType.INTEGER, resultDeclaration.type)
        assertTrue(resultDeclaration.nullable)
        assertEquals(1.0, resultDeclaration.minimum)
    }

    @Test
    fun `the original tree is not changed`() {
        val original = property("optional", string())
        val root = ObjectNode("Root", isRoot = true).apply { properties += original }

        val result = RequireAllProperties().transform(root) as ObjectNode

        assertNotSame(original, result.property("optional"))
        assertFalse(original.required)
    }
}
