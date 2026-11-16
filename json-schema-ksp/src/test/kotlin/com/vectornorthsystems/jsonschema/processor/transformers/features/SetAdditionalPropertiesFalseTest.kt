package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.mockk
import kotlin.test.*

class SetAdditionalPropertiesFalseTest {

    private fun property(name: String, declaration: Node, required: Boolean = false) =
        PropertyNode(name = name, declaration = declaration, source = mockk<KSNode>(), required = required)

    private fun string() = PrimitiveNode(PrimitiveType.STRING)

    private fun ObjectNode.property(name: String) = properties.single { it.name == name }

    @Test
    fun `the root gets additionalProperties false, whatever it had before`() {
        listOf(null, true, false).forEach { before ->
            val root = ObjectNode("Root", isRoot = true, additionalProperties = before)

            val result = SetAdditionalPropertiesFalse().transform(root) as ObjectNode

            assertEquals(false, result.additionalProperties, "was $before")
        }
    }

    @Test
    fun `every nested object gets additionalProperties false`() {
        // Root.child: Child, Root.items: List<Item>, Root.byKey: Map<String, Value>.
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("child", ObjectNode("Child"))
            properties += property("items", ArrayNode(ObjectNode("Item")))
            properties += property("byKey", MapNode(ObjectNode("Value"), mockk()))
        }

        val result = SetAdditionalPropertiesFalse().transform(root) as ObjectNode

        val child = assertIs<ObjectNode>(result.property("child").declaration)
        val item = assertIs<ObjectNode>(assertIs<ArrayNode>(result.property("items").declaration).declaration)
        val value = assertIs<ObjectNode>(assertIs<MapNode>(result.property("byKey").declaration).declaration)
        listOf(result, child, item, value).forEach {
            assertEquals(false, it.additionalProperties, "${it.name} should have additionalProperties false")
        }
    }

    @Test
    fun `maps stay maps`() {
        // A map's values are its `additionalProperties`, so it's left to the dialect to reject it, not turned into an
        // object without properties.
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("byKey", MapNode(string(), mockk()))
        }

        val result = SetAdditionalPropertiesFalse().transform(root) as ObjectNode

        val map = assertIs<MapNode>(result.property("byKey").declaration)
        assertIs<PrimitiveNode>(map.declaration)
    }

    @Test
    fun `properties are kept`() {
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("name", string(), required = true)
            properties += property("age", PrimitiveNode(PrimitiveType.INTEGER))
        }

        val result = SetAdditionalPropertiesFalse().transform(root) as ObjectNode

        assertEquals(listOf("name", "age"), result.properties.map { it.name })
        assertEquals(listOf(true, false), result.properties.map { it.required })
    }

    @Test
    fun `references point at the object with additionalProperties false`() {
        // Employee.manager: Employee
        val employee = ObjectNode("Employee").apply { referenced = true }
        employee.properties += property("manager", RefNode(employee))
        val root = ObjectNode("Org", isRoot = true).apply { properties += property("head", employee) }

        val result = SetAdditionalPropertiesFalse().transform(root) as ObjectNode

        val resultEmployee = assertIs<ObjectNode>(result.property("head").declaration)
        val target = assertIs<RefNode>(resultEmployee.property("manager").declaration).target
        assertSame(resultEmployee, target)
        assertEquals(false, target.additionalProperties)
        assertTrue(target.referenced)
    }

    @Test
    fun `the original tree is not changed`() {
        val child = ObjectNode("Child", additionalProperties = true)
        val root = ObjectNode("Root", isRoot = true).apply { properties += property("child", child) }

        val result = SetAdditionalPropertiesFalse().transform(root) as ObjectNode

        assertNotSame(root, result)
        assertNull(root.additionalProperties)
        assertEquals(true, child.additionalProperties)
    }
}
