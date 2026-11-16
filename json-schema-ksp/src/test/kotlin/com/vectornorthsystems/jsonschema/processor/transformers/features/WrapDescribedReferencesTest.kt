package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.mockk
import kotlin.test.*

class WrapDescribedReferencesTest {

    private fun property(name: String, declaration: Node) =
        PropertyNode(name = name, declaration = declaration, source = mockk<KSNode>(), required = true)

    private fun string() = PrimitiveNode(PrimitiveType.STRING)

    private fun ObjectNode.property(name: String) = properties.single { it.name == name }

    @Test
    fun `properties are marked at every depth`() {
        // Root.child: Child, Root.items: List<Item>, Child.self: Child.
        val child = ObjectNode("Child").apply { properties += property("name", string()) }
        child.properties += property("self", RefNode(child))
        val item = ObjectNode("Item").apply { properties += property("name", string()) }
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("child", child)
            properties += property("items", ArrayNode(item))
        }

        val result = WrapDescribedReferences().transform(root) as ObjectNode

        val resultChild = result.property("child").declaration as ObjectNode
        val resultItem = (result.property("items").declaration as ArrayNode).declaration as ObjectNode
        val all = result.properties + resultChild.properties + resultItem.properties
        assertEquals(5, all.size)
        all.forEach { assertTrue(it.wrapDescribedReference, "${it.name} should be marked") }

        // The reference points at the transformed object.
        assertSame(resultChild, (resultChild.property("self").declaration as RefNode).target)
    }

    @Test
    fun `the rest of the property is copied unchanged`() {
        val source = mockk<KSNode>()
        val target = ObjectNode("Target", isRoot = true)
        val original = PropertyNode("self", RefNode(target), source, required = false).apply {
            title = "Self"
            description = "Itself"
        }
        target.properties += original

        val result = (WrapDescribedReferences().transform(target) as ObjectNode).property("self")

        assertEquals("self", result.name)
        assertSame(source, result.source)
        assertFalse(result.required)
        assertEquals("Self", result.title)
        assertEquals("Itself", result.description)
        assertIs<RefNode>(result.declaration)
    }

    @Test
    fun `the original tree is not changed`() {
        val original = property("name", string())
        val root = ObjectNode("Root", isRoot = true).apply { properties += original }

        val result = WrapDescribedReferences().transform(root) as ObjectNode

        assertNotSame(original, result.property("name"))
        assertFalse(original.wrapDescribedReference)
    }
}
