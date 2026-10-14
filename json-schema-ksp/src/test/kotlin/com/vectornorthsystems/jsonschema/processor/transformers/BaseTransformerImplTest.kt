package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.mockk
import kotlin.test.*

/**
 * Records the nodes each hook receives, delegating the copy to [BaseTransformerImpl].
 */
internal class TestTransformerImpl : BaseTransformerImpl() {
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
    is ArrayNode -> declaration.allNodes() + prefixItems.flatMap { it.allNodes() }
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
            assertEquals(original.recursive, copy.recursive)
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
            assertEquals(original.prefixItems.size, copy.prefixItems.size)
            original.prefixItems.zip(copy.prefixItems).forEach { (o, c) -> assertCopy(o, c, copies) }
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
            recursive = true
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
                prefixItems = listOf(PrimitiveNode(PrimitiveType.INTEGER), PrimitiveNode(PrimitiveType.BOOLEAN))
                minItems = 1
                maxItems = 3
            }.common()),
            property("scores", MapNode(PrimitiveNode(PrimitiveType.NUMBER)).common()),
            property("color", color.common()),
            property("expansions", ArrayNode(RefNode(root).common())),
        ).map { it.common() }

        return root
    }

    @Test
    fun transform() {
        val source = tree()
        val transformer = TestTransformerImpl()

        val result = transformer.transform(root = source)

        assertCopy(source, result)

        // Every node is visited once, through its hook.
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
        val source = tree()
        val snapshot = BaseTransformerImpl().transform(root = source)

        TestTransformerImpl().transform(root = source)

        // The source still matches the copy taken before the transform.
        assertCopy(source, snapshot)
    }

    @Test
    fun transformNode() {
        val transformer = TestTransformerImpl()
        val nodes = listOf(
            PrimitiveNode(PrimitiveType.BOOLEAN),
            ArrayNode(PrimitiveNode(PrimitiveType.STRING)),
            MapNode(PrimitiveNode(PrimitiveType.STRING)),
            ObjectNode(name = "Empty"),
            property("flag", PrimitiveNode(PrimitiveType.BOOLEAN)),
        )

        nodes.forEach { node ->
            transformer.visited.clear()
            val copy = transformer.transform(node)

            assertCopy(node, copy)
            assertSame(node, transformer.visited.last(), "${node::class.simpleName} not dispatched to its hook")
        }
    }

    @Test
    fun transformProperty() {
        val source = property("name", PrimitiveNode(PrimitiveType.STRING).common(), required = true).common()

        val copy = BaseTransformerImpl().transformProperty(source)

        assertCopy(source, copy)
    }

    @Test
    fun transformPrimitiveNode() {
        val source = PrimitiveNode(PrimitiveType.NUMBER, enum = listOf("1", "2")).apply {
            format = "double"
            minimum = -1.5
            maximum = 1.5
        }.common()

        val copy = BaseTransformerImpl().transformPrimitiveNode(source)

        assertCopy(source, copy)
    }

    @Test
    fun transformObjectNode() {
        val source = ObjectNode(name = "Player", isRoot = true).apply {
            recursive = true
            defName = "Player2"
            properties += property("name", PrimitiveNode(PrimitiveType.STRING))
            properties += property("score", PrimitiveNode(PrimitiveType.INTEGER), required = true)
        }.common()

        val copy = BaseTransformerImpl().transformObjectNode(source)

        assertCopy(source, copy)
    }

    @Test
    fun transformRefNode() {
        val node = ObjectNode(name = "Node").apply { recursive = true }
        val ref = RefNode(node).common()
        node.properties += property("children", ArrayNode(ref))

        val copy = BaseTransformerImpl().transformObjectNode(node)

        val copiedRef = assertIs<RefNode>(assertIs<ArrayNode>(copy.properties.single().declaration).declaration)
        assertNotSame(ref, copiedRef)
        assertSame(copy, copiedRef.target)
        assertEquals(ref.title, copiedRef.title)
        assertEquals(ref.description, copiedRef.description)
        assertEquals(ref.nullable, copiedRef.nullable)
    }

    @Test
    fun transformRefNodeOutsideItsTarget() {
        // The target hasn't been copied, so there's nothing to point to.
        assertFailsWith<NoSuchElementException> {
            BaseTransformerImpl().transformRefNode(RefNode(ObjectNode(name = "Unseen")))
        }
    }

    @Test
    fun transformArrayNode() {
        val source = ArrayNode(PrimitiveNode(PrimitiveType.STRING).common()).apply {
            prefixItems = listOf(PrimitiveNode(PrimitiveType.INTEGER).common(), ObjectNode(name = "First"))
            minItems = 2
            maxItems = 5
        }.common()

        val copy = BaseTransformerImpl().transformArrayNode(source)

        assertCopy(source, copy)
    }

    @Test
    fun transformMapNode() {
        val source = MapNode(ArrayNode(PrimitiveNode(PrimitiveType.BOOLEAN)).common()).common()

        val copy = BaseTransformerImpl().transformMapNode(source)

        assertCopy(source, copy)
    }
}
