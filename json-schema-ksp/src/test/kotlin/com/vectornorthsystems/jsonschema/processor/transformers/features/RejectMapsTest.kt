package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import com.vectornorthsystems.jsonschema.processor.transformers.CompositeTransformer
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class RejectMapsTest {

    private val dialectName = "TestDialect"
    private val message = "Map isn't supported by the $dialectName dialect, which requires every object to have " +
            "fixed properties. Use a data class if the keys are known, or a List of key-value entries."

    private val logger = mockk<KSPLogger>(relaxed = true)

    // Given a transformer containing a RejectMaps feature:
    private val transformer = CompositeTransformer(RejectMaps(dialectName, logger))

    private fun property(name: String, declaration: Node) =
        PropertyNode(name, declaration, mockk<KSNode>(relaxed = true), required = true)

    @Test
    fun `no error is logged when there's no map`() {
        // Given an object whose properties hold a primitive, an array and a nested object, but no map:
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("name", PrimitiveNode(PrimitiveType.STRING))
            properties += property("tags", ArrayNode(PrimitiveNode(PrimitiveType.STRING)))
            properties += property("child", ObjectNode("Child").apply {
                properties += property("count", PrimitiveNode(PrimitiveType.INTEGER))
            })
        }

        // When transforming...
        transformer.transform(root)

        // We expect no error at all to be logged, which would fail the build.
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `an error is logged at the map's source`() {
        // Given an object with a map property:
        val source = mockk<KSNode>(relaxed = true)
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("byKey", MapNode(PrimitiveNode(PrimitiveType.STRING), source))
        }

        // When transforming...
        transformer.transform(root)

        // We expect one error, pointing at the map, which fails the build.
        verify(exactly = 1) { logger.error(message, source) }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `a map nested in an array of objects is reported`() {
        // Given Root.items: List<Item>, where Item.byKey: Map<String, String>:
        val source = mockk<KSNode>(relaxed = true)
        val item = ObjectNode("Item").apply {
            properties += property("byKey", MapNode(PrimitiveNode(PrimitiveType.STRING), source))
        }
        val root = ObjectNode("Root", isRoot = true).apply { properties += property("items", ArrayNode(item)) }

        // When transforming...
        transformer.transform(root)

        // We expect the walk to reach the map and report it.
        verify(exactly = 1) { logger.error(message, source) }
    }

    @Test
    fun `a map inside a map's values is reported too`() {
        // Given Root.byKey: Map<String, Map<String, Int>>:
        val outer = mockk<KSNode>(relaxed = true)
        val inner = mockk<KSNode>(relaxed = true)
        val root = ObjectNode("Root", isRoot = true).apply {
            properties += property("byKey", MapNode(MapNode(PrimitiveNode(PrimitiveType.INTEGER), inner), outer))
        }

        // When transforming...
        transformer.transform(root)

        // We expect both maps to be reported, each at its own source.
        verify(exactly = 1) { logger.error(message, outer) }
        verify(exactly = 1) { logger.error(message, inner) }
    }
}
