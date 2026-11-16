package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.ArrayNode
import com.vectornorthsystems.jsonschema.processor.node.MapNode
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.node.PrimitiveNode
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import com.vectornorthsystems.jsonschema.processor.node.RefNode
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransformersTest {

    @Test
    fun `each call gives a new transformer`() {
        // Given a dialect...
        // When asking for its transformer twice...
        val first = SchemaTarget.GEMINI.transformer(mockk(relaxed = true))
        val second = SchemaTarget.GEMINI.transformer(mockk(relaxed = true))

        // We expect two instances, since a transformer remembers the objects of the one tree it copied:
        assertNotSame(first, second)
    }

    @Test
    fun `each OpenAI transformer has its own features`() {
        // Given two OpenAI transformers...
        val first = assertIs<CompositeTransformer>(SchemaTarget.OPENAI.transformer(mockk(relaxed = true)))
        val second = assertIs<CompositeTransformer>(SchemaTarget.OPENAI.transformer(mockk(relaxed = true)))

        // We expect no feature shared, since each remembers the objects of the one tree it copied:
        first.features.zip(second.features).forEach { (a, b) -> assertNotSame(a, b) }
    }

    @Test
    fun `a map is an error for OpenAI, naming the dialect, and not for Gemini`() {
        // Given an object with a map property:
        val source = mockk<KSNode>()
        fun root() = ObjectNode(name = "Scores", isRoot = true).apply {
            properties += PropertyNode("byName", MapNode(PrimitiveNode(PrimitiveType.INTEGER), source), mockk(), true)
        }
        val gemini = mockk<KSPLogger>(relaxed = true)
        val openAi = mockk<KSPLogger>(relaxed = true)

        // When transforming it for each dialect...
        SchemaTarget.GEMINI.transformer(gemini).transform(root())
        SchemaTarget.OPENAI.transformer(openAi).transform(root())

        // We expect Gemini to accept it, and OpenAI to report it at the map's source:
        verify(exactly = 0) { gemini.error(any(), any()) }
        verify(exactly = 1) { openAi.error(match { "OPENAI dialect" in it }, source) }
    }

    @Test
    fun `OpenAI keeps the properties and references`() {
        // Given an object with an optional property, and a list of references to itself:
        val root = ObjectNode(name = "Node", isRoot = true).apply { referenced = true }
        root.properties += PropertyNode("name", PrimitiveNode(PrimitiveType.STRING), mockk<KSNode>(), required = false)
        root.properties += PropertyNode("children", ArrayNode(RefNode(root)), mockk<KSNode>(), required = false)

        // When transforming it for OpenAI...
        val result = assertIs<ObjectNode>(SchemaTarget.OPENAI.transformer(mockk(relaxed = true)).transform(root))

        // We expect both properties, now required, and no additional properties:
        assertEquals(listOf("name", "children"), result.properties.map { it.name })
        assertTrue(result.properties.all { it.required })
        assertEquals(false, result.additionalProperties)

        // And the reference to point at the object in the result, not at the original or a discarded copy:
        val ref = assertIs<RefNode>(assertIs<ArrayNode>(result.properties[1].declaration).declaration)
        assertSame(result, ref.target)

        // And the source unchanged:
        assertFalse(root.properties.any { it.required })
        assertEquals(null, root.additionalProperties)
    }

    @Test
    fun `described references are marked to be wrapped for OpenAI, and not for Gemini`() {
        // Given an object with a described reference to itself:
        fun root() = ObjectNode(name = "Node", isRoot = true).apply {
            referenced = true
            properties += PropertyNode("next", RefNode(this), mockk<KSNode>(), required = true).apply {
                description = "The next one"
            }
        }

        // When transforming it for each dialect...
        val gemini = assertIs<ObjectNode>(SchemaTarget.GEMINI.transformer(mockk(relaxed = true)).transform(root()))
        val openAi = assertIs<ObjectNode>(SchemaTarget.OPENAI.transformer(mockk(relaxed = true)).transform(root()))

        // We expect only OpenAI's to be marked, since it rejects a description next to a $ref:
        assertFalse(gemini.properties.single().wrapDescribedReference)
        assertTrue(openAi.properties.single().wrapDescribedReference)
        // And the description kept:
        assertEquals("The next one", openAi.properties.single().description)
    }
}
