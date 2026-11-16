package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import com.vectornorthsystems.jsonschema.processor.transformers.features.RequireAllProperties
import com.vectornorthsystems.jsonschema.processor.transformers.features.SetAdditionalPropertiesFalse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.*

/**
 * The composite copies the tree, then folds it through every feature in order.
 */
class CompositeTransformerTest {

    private fun property(name: String, declaration: Node, required: Boolean = false) =
        PropertyNode(name = name, declaration = declaration, source = mockk<KSNode>(), required = required)

    /**
     * Root.name: String?, Root.child: Child, where Child.count: Int?.
     */
    private fun tree() = ObjectNode("Root", isRoot = true).apply {
        properties += property("name", PrimitiveNode(PrimitiveType.STRING))
        properties += property("child", ObjectNode("Child").apply {
            properties += property("count", PrimitiveNode(PrimitiveType.INTEGER))
        })
    }

    /**
     * A feature that returns [output] for whatever it's given.
     */
    private fun feature(output: Node) = mockk<Transformer> { every { transform(any()) } returns output }

    @Test
    fun `without features, the tree is copied`() {
        // Given a composite with no features, as for Gemini:
        val root = tree()

        // When transforming...
        val result = CompositeTransformer().transform(root)

        // We expect a copy, not the original:
        assertNotSame(root, result)
        result as ObjectNode
        assertEquals(listOf("name", "child"), result.properties.map { it.name })
        assertNotSame(root.properties[1].declaration, result.properties[1].declaration)
    }

    @Test
    fun `every feature is applied once, in order, to the output of the previous one`() {
        // Given three features, each returning its own node:
        val first = ObjectNode("First")
        val second = ObjectNode("Second")
        val third = ObjectNode("Third")
        val a = feature(first)
        val b = feature(second)
        val c = feature(third)
        val root = tree()

        // When transforming...
        val result = CompositeTransformer(a, b, c).transform(root)

        // We expect each feature to get what the previous one returned:
        verifyOrder {
            a.transform(any())
            b.transform(first)
            c.transform(second)
        }
        verify(exactly = 1) { a.transform(any()) }
        verify(exactly = 1) { b.transform(any()) }
        verify(exactly = 1) { c.transform(any()) }

        // And the result to be what the last one returned:
        assertSame(third, result)
    }

    @Test
    fun `the first feature gets a copy of the tree, not the original`() {
        // Given a single feature:
        val root = tree()
        val received = mutableListOf<Node>()
        val a = mockk<Transformer> { every { transform(capture(received)) } answers { firstArg() } }

        // When transforming...
        CompositeTransformer(a).transform(root)

        // We expect it to get a copy of the tree, so the original is never at risk:
        val copy = assertIs<ObjectNode>(received.single())
        assertNotSame(root, copy)
        assertEquals("Root", copy.name)
        assertEquals(listOf("name", "child"), copy.properties.map { it.name })
    }

    @Test
    fun `the changes of every feature are kept, whatever the order`() {
        listOf(
            { CompositeTransformer(RequireAllProperties(), SetAdditionalPropertiesFalse()) },
            { CompositeTransformer(SetAdditionalPropertiesFalse(), RequireAllProperties()) },
        ).forEach { composite ->
            // Given the real features, in either order:
            val root = tree()

            // When transforming...
            val result = composite().transform(root) as ObjectNode

            // We expect both features' changes on every object, a later feature not undoing an earlier one:
            val child = result.properties.single { it.name == "child" }.declaration as ObjectNode
            listOf(result, child).forEach { node ->
                assertEquals(false, node.additionalProperties, "${node.name} should have additionalProperties false")
                node.properties.forEach { assertTrue(it.required, "${node.name}.${it.name} should be required") }
            }

            // And the original unchanged:
            assertNull(root.additionalProperties)
            assertTrue(root.properties.none { it.required })
        }
    }
}
