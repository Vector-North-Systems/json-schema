package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.mockk
import kotlin.test.*

/**
 * Records the nodes each hook receives, delegating the copy to [BaseTransformerImpl].
 * It changes nothing, so it also stands for the base itself, which is abstract.
 */
internal class TestTransformerImpl : BaseTransformerImpl() {
    // Any dialect: the base copies the tree the same way for all of them.
    override val target = SchemaTarget.GEMINI

    val visited = mutableListOf<Node>()

    override fun transformProperty(node: PropertyNode) = super.transformProperty(node).also { visited += node }
    override fun transformPrimitiveNode(node: PrimitiveNode) =
        super.transformPrimitiveNode(node).also { visited += node }

    override fun transformObjectNode(node: ObjectNode) = super.transformObjectNode(node).also { visited += node }
    override fun transformRefNode(node: RefNode) = super.transformRefNode(node).also { visited += node }
    override fun transformArrayNode(node: ArrayNode) = super.transformArrayNode(node).also { visited += node }
    override fun transformMapNode(node: MapNode) = super.transformMapNode(node).also { visited += node }
}

private fun property(name: String, declaration: Node, required: Boolean = false) =
    PropertyNode(name = name, declaration = declaration, source = mockk<KSNode>(), required = required)

private fun <T : Node> T.common(): T = apply {
    title = "Title of ${this::class.simpleName}"
    description = "Description of ${this::class.simpleName}"
    nullable = true
}

/**
 * Collects every node in the tree, without following references.
 */
private fun Node.allNodes(): List<Node> = listOf(this) + when (this) {
    is ObjectNode -> properties.flatMap { it.allNodes() }
    is PropertyNode -> declaration.allNodes()
    is ArrayNode -> declaration.allNodes()
    is MapNode -> declaration.allNodes()
    is PrimitiveNode, is RefNode -> emptyList()
}

/**
 * Asserts [copy] is a new instance equal to [original], down the whole tree. References in the copy must point to the
 * copied objects, looked up in [copies].
 */
private fun assertCopy(original: Node, copy: Node, copies: MutableMap<ObjectNode, ObjectNode> = mutableMapOf()) {
    assertNotSame(original, copy)
    assertEquals(original::class, copy::class)
    assertEquals(original.title, copy.title)
    assertEquals(original.description, copy.description)
    assertEquals(original.nullable, copy.nullable)

    when (original) {
        is ObjectNode -> {
            copy as ObjectNode
            copies[original] = copy
            assertEquals(original.name, copy.name)
            assertEquals(original.isRoot, copy.isRoot)
            assertEquals(original.referenced, copy.referenced)
            assertEquals(original.defName, copy.defName)
            assertEquals(original.properties.size, copy.properties.size)
            original.properties.zip(copy.properties).forEach { (o, c) -> assertCopy(o, c, copies) }
        }

        is PropertyNode -> {
            copy as PropertyNode
            assertEquals(original.name, copy.name)
            assertEquals(original.required, copy.required)
            assertSame(original.source, copy.source)
            assertCopy(original.declaration, copy.declaration, copies)
        }

        is PrimitiveNode -> {
            copy as PrimitiveNode
            assertEquals(original.type, copy.type)
            assertEquals(original.enum, copy.enum)
            assertEquals(original.format, copy.format)
            assertEquals(original.minimum, copy.minimum)
            assertEquals(original.maximum, copy.maximum)
        }

        is ArrayNode -> {
            copy as ArrayNode
            assertEquals(original.minItems, copy.minItems)
            assertEquals(original.maxItems, copy.maxItems)
            assertCopy(original.declaration, copy.declaration, copies)
        }

        is MapNode -> {
            copy as MapNode
            assertCopy(original.declaration, copy.declaration, copies)
        }

        is RefNode -> {
            copy as RefNode
            assertSame(copies.getValue(original.target), copy.target)
        }
    }
}

/**
 * Base transformer should call all the hooks, and produce a copy.
 */
class BaseTransformerImplTest {

    /**
     * A tree with every kind of node, and every field set.
     */
    private fun tree(): ObjectNode {
        val root = ObjectNode(name = "Game", isRoot = true).apply {
            referenced = true
            defName = "GameDef"
        }.common()

        val color = ObjectNode(name = "Color").common()
        color.properties += property("hex", PrimitiveNode(PrimitiveType.STRING).apply { format = "color" }.common())

        root.properties += listOf(
            property("name", PrimitiveNode(PrimitiveType.STRING).common(), required = true),
            property("players", PrimitiveNode(PrimitiveType.INTEGER).apply {
                minimum = 1.0
                maximum = 8.0
            }.common()),
            property("level", PrimitiveNode(PrimitiveType.STRING, enum = listOf("EASY", "HARD")).common()),
            property("rules", ArrayNode(PrimitiveNode(PrimitiveType.STRING)).apply {
                minItems = 1
                maxItems = 3
            }.common()),
            property("scores", MapNode(PrimitiveNode(PrimitiveType.NUMBER)).common()),
            property("color", color.common()),
            property("expansions", ArrayNode(RefNode(root).common())),
        ).map { it.common() }

        return root
    }

    // region transform

    @Test
    fun transform() {
        // Given a tree with every kind of node, and a transformer that records the nodes it visits:
        val source = tree()
        val transformer = TestTransformerImpl()

        // When transforming...
        val result = transformer.transform(root = source)

        // We expect a copy of the whole tree:
        assertCopy(source, result)

        // And every node to be visited once, through its hook:
        val nodes = source.allNodes()
        assertEquals(nodes.size, transformer.visited.size)
        nodes.forEach { node ->
            assertEquals(
                1,
                transformer.visited.count { it === node },
                "${node::class.simpleName} not visited once"
            )
        }
    }

    @Test
    fun transformLeavesSourceUnchanged() {
        // Given a tree, and a copy of it taken before transforming:
        val source = tree()
        val snapshot = TestTransformerImpl().transform(root = source)

        // When transforming the tree...
        TestTransformerImpl().transform(root = source)

        // We expect the source to still match the copy taken before:
        assertCopy(source, snapshot)
    }

    @Test
    fun transformNode() {
        // Given a transformer that records the nodes it visits:
        val transformer = TestTransformerImpl()

        // And one node of each kind:
        val nodes = listOf(
            PrimitiveNode(PrimitiveType.BOOLEAN),
            ArrayNode(PrimitiveNode(PrimitiveType.STRING)),
            MapNode(PrimitiveNode(PrimitiveType.STRING)),
            ObjectNode(name = "Empty"),
            property("flag", PrimitiveNode(PrimitiveType.BOOLEAN)),
        )

        nodes.forEach { node ->
            transformer.visited.clear()

            // When transforming each...
            val copy = transformer.transform(node)

            // We expect a copy, and the node to be dispatched to the hook of its kind:
            assertCopy(node, copy)
            assertSame(node, transformer.visited.last(), "${node::class.simpleName} not dispatched to its hook")
        }
    }

    // endregion

    // region Hooks

    @Test
    fun transformProperty() {
        // Given a required property with a title, a description and a nullable type:
        val source = property("name", PrimitiveNode(PrimitiveType.STRING).common(), required = true).common()

        // When transforming...
        val copy = TestTransformerImpl().transformProperty(source)

        // We expect an equal copy:
        assertCopy(source, copy)
    }

    @Test
    fun transformPrimitiveNode() {
        // Given a primitive with every field set:
        val source = PrimitiveNode(PrimitiveType.NUMBER, enum = listOf("1", "2")).apply {
            format = "double"
            minimum = -1.5
            maximum = 1.5
        }.common()

        // When transforming...
        val copy = TestTransformerImpl().transformPrimitiveNode(source)

        // We expect an equal copy:
        assertCopy(source, copy)
    }

    @Test
    fun transformObjectNode() {
        // Given a root object that is referenced, with a custom definition name and two properties:
        val source = ObjectNode(name = "Player", isRoot = true).apply {
            referenced = true
            defName = "Player2"
            properties += property("name", PrimitiveNode(PrimitiveType.STRING))
            properties += property("score", PrimitiveNode(PrimitiveType.INTEGER), required = true)
        }.common()

        // When transforming...
        val copy = TestTransformerImpl().transformObjectNode(source)

        // We expect an equal copy:
        assertCopy(source, copy)
    }

    @Test
    fun transformRefNode() {
        // Given an object whose property is a list of references to itself:
        val node = ObjectNode(name = "Node").apply { referenced = true }
        val ref = RefNode(node).common()
        node.properties += property("children", ArrayNode(ref))

        // When transforming the object...
        val copy = TestTransformerImpl().transformObjectNode(node)

        // We expect a new reference that points to the copied object, not to the original:
        val copiedRef = assertIs<RefNode>(assertIs<ArrayNode>(copy.properties.single().declaration).declaration)
        assertNotSame(ref, copiedRef)
        assertSame(copy, copiedRef.target)

        // And the same title, description and nullability:
        assertEquals(ref.title, copiedRef.title)
        assertEquals(ref.description, copiedRef.description)
        assertEquals(ref.nullable, copiedRef.nullable)
    }

    @Test
    fun transformRefNodeOutsideItsTarget() {
        // Given a reference to an object that hasn't been copied, so there's nothing to point to...
        // When transforming it...
        // We expect a failure:
        assertFailsWith<NoSuchElementException> {
            TestTransformerImpl().transformRefNode(RefNode(ObjectNode(name = "Unseen")))
        }
    }

    @Test
    fun transformArrayNode() {
        // Given an array with item limits, and a title, a description and a nullable type:
        val source = ArrayNode(PrimitiveNode(PrimitiveType.STRING).common()).apply {
            minItems = 2
            maxItems = 5
        }.common()

        // When transforming...
        val copy = TestTransformerImpl().transformArrayNode(source)

        // We expect an equal copy:
        assertCopy(source, copy)
    }

    @Test
    fun transformMapNode() {
        // Given a map of arrays of booleans:
        val source = MapNode(ArrayNode(PrimitiveNode(PrimitiveType.BOOLEAN)).common()).common()

        // When transforming...
        val copy = TestTransformerImpl().transformMapNode(source)

        // We expect an equal copy:
        assertCopy(source, copy)
    }

    // endregion
}
