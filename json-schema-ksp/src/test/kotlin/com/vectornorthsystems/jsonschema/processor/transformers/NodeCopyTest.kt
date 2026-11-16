package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.mockk
import kotlin.test.*

/**
 * The copy extensions should produce new instances with the same fields, unless overridden.
 */
class NodeCopyTest {

    private fun <T : Node> T.common(): T = apply {
        title = "Title"
        description = "Description"
        nullable = true
    }

    // region copyCommon

    @Test
    fun copyCommon() {
        // Given a node with every common field set, and a blank node of another kind:
        val original = PrimitiveNode(PrimitiveType.STRING).common()
        val target = MapNode(PrimitiveNode(PrimitiveType.STRING), mockk(relaxed = true))

        // When copying the common fields...
        val result = target.copyCommon(original)

        // We expect them on the target, which is returned for chaining:
        assertSame(target, result)
        assertEquals("Title", result.title)
        assertEquals("Description", result.description)
        assertTrue(result.nullable)
    }

    @Test
    fun copyCommonDefaults() {
        // Given a node with no common fields set, and a target that has them:
        val original = PrimitiveNode(PrimitiveType.STRING)
        val target = PrimitiveNode(PrimitiveType.STRING).common()

        // When copying the common fields...
        target.copyCommon(original)

        // We expect the target's to be cleared, not kept:
        assertNull(target.title)
        assertNull(target.description)
        assertFalse(target.nullable)
    }

    // endregion

    // region PropertyNode.copy

    @Test
    fun propertyCopy() {
        // Given a required property with every field set:
        val declaration = PrimitiveNode(PrimitiveType.STRING)
        val source = mockk<KSNode>()
        val original = PropertyNode(name = "name", declaration = declaration, source = source, required = true).common()
        original.wrapDescribedReference = true

        // When copying it...
        val copy = original.copy()

        // We expect a new instance with the same fields, sharing the declaration and source:
        assertNotSame(original, copy)
        assertEquals("name", copy.name)
        assertSame(declaration, copy.declaration)
        assertSame(source, copy.source)
        assertTrue(copy.required)
        assertTrue(copy.wrapDescribedReference)
        assertEquals("Title", copy.title)
        assertEquals("Description", copy.description)
        assertTrue(copy.nullable)
    }

    @Test
    fun propertyCopyOverrides() {
        // Given an optional property:
        val original = PropertyNode(
            name = "name",
            declaration = PrimitiveNode(PrimitiveType.STRING),
            source = mockk<KSNode>(),
            required = false
        ).common()
        val declaration = PrimitiveNode(PrimitiveType.INTEGER)
        val source = mockk<KSNode>()

        // When copying it with every field overridden...
        val copy = original.copy(name = "count", declaration = declaration, source = source, required = true)

        // We expect the new values, and the common fields kept:
        assertEquals("count", copy.name)
        assertSame(declaration, copy.declaration)
        assertSame(source, copy.source)
        assertTrue(copy.required)
        assertEquals("Title", copy.title)
        assertTrue(copy.nullable)

        // And the original unchanged:
        assertEquals("name", original.name)
        assertFalse(original.required)
    }

    // endregion

    // region PrimitiveNode.copy

    @Test
    fun primitiveCopy() {
        // Given a primitive with every field set:
        val original = PrimitiveNode(PrimitiveType.NUMBER, enum = listOf("1", "2")).apply {
            format = "double"
            minimum = -1.5
            maximum = 1.5
        }.common()

        // When copying it...
        val copy = original.copy()

        // We expect a new instance with the same fields:
        assertNotSame(original, copy)
        assertEquals(PrimitiveType.NUMBER, copy.type)
        assertEquals(listOf("1", "2"), copy.enum)
        assertEquals("double", copy.format)
        assertEquals(-1.5, copy.minimum)
        assertEquals(1.5, copy.maximum)
        assertEquals("Title", copy.title)
        assertEquals("Description", copy.description)
        assertTrue(copy.nullable)
    }

    @Test
    fun primitiveCopyOverrides() {
        // Given an enum primitive with a format and bounds:
        val original = PrimitiveNode(PrimitiveType.STRING, enum = listOf("A")).apply {
            format = "x"
            minimum = 0.0
            maximum = 10.0
        }.common()

        // When copying it with another type and no enum...
        val copy = original.copy(type = PrimitiveType.INTEGER, enum = null)

        // We expect the new values, and the other fields kept:
        assertEquals(PrimitiveType.INTEGER, copy.type)
        assertNull(copy.enum)
        assertEquals("x", copy.format)
        assertEquals(0.0, copy.minimum)
        assertEquals(10.0, copy.maximum)
        assertEquals("Title", copy.title)
        assertEquals("Description", copy.description)
        assertTrue(copy.nullable)

        // And the original unchanged:
        assertEquals(PrimitiveType.STRING, original.type)
        assertEquals(listOf("A"), original.enum)
    }

    // endregion

    // region ObjectNode.copy

    @Test
    fun objectCopy() {
        // Given a referenced root object with every field set:
        val original = ObjectNode(name = "Game", isRoot = true, additionalProperties = true).apply {
            referenced = true
            defName = "GameDef"
        }.common()

        // When copying it...
        val copy = original.copy()

        // We expect a new instance with the same fields:
        assertNotSame(original, copy)
        assertEquals("Game", copy.name)
        assertTrue(copy.isRoot)
        assertEquals(true, copy.additionalProperties)
        assertTrue(copy.referenced)
        assertEquals("GameDef", copy.defName)
        assertEquals("Title", copy.title)
        assertEquals("Description", copy.description)
        assertTrue(copy.nullable)
    }

    @Test
    fun objectCopyOverrides() {
        // Given a referenced object with a custom definition name:
        val original = ObjectNode(name = "Game").apply {
            referenced = true
            defName = "GameDef"
        }.common()

        // When copying it with every constructor field overridden...
        val copy = original.copy(name = "Match", isRoot = true, additionalProperties = false)

        // We expect the new values:
        assertEquals("Match", copy.name)
        assertTrue(copy.isRoot)
        assertEquals(false, copy.additionalProperties)

        // The other fields kept, and the definition name not derived from the new name:
        assertTrue(copy.referenced)
        assertEquals("GameDef", copy.defName)
        assertEquals("Title", copy.title)
        assertEquals("Description", copy.description)
        assertTrue(copy.nullable)

        // And the original unchanged:
        assertEquals("Game", original.name)
        assertFalse(original.isRoot)
        assertNull(original.additionalProperties)
    }

    @Test
    fun objectCopyLeavesPropertiesOut() {
        // Given an object with a property:
        val original = ObjectNode(name = "Game").apply {
            properties += PropertyNode("name", PrimitiveNode(PrimitiveType.STRING), mockk<KSNode>(), required = true)
        }

        // When copying it...
        val copy = original.copy()

        // We expect no properties: the transformer copies them itself, so the copy can be registered for references
        // first. Copying a transformed object again loses them.
        assertTrue(copy.properties.isEmpty())
        assertEquals(1, original.properties.size)
    }

    // endregion
}
