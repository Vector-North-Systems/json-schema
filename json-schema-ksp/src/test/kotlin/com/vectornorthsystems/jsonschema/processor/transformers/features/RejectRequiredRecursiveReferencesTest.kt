package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.*
import com.vectornorthsystems.jsonschema.processor.transformers.CompositeTransformer
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class RejectRequiredRecursiveReferencesTest {

    private val dialectName = "TestDialect"

    private val logger = mockk<KSPLogger>(relaxed = true)

    // Given a transformer containing the feature:
    private val transformer = CompositeTransformer(RejectRequiredRecursiveReferences(dialectName, logger))

    private fun property(name: String, declaration: Node, source: KSNode = mockk(relaxed = true), required: Boolean = true) =
        PropertyNode(name, declaration, source, required)

    private fun string() = PrimitiveNode(PrimitiveType.STRING)

    private fun nullableRef(target: ObjectNode) = RefNode(target).apply { nullable = true }

    private fun message(owner: String, property: String) =
        "A required property that leads back to its own class, $owner, isn't supported by the $dialectName dialect. " +
                "Make it optional with a default: `$property: ... = null`."

    @Test
    fun `a required nullable reference to the root is reported`() {
        // Given Category.promoted: Category?, required:
        val source = mockk<KSNode>(relaxed = true)
        val root = ObjectNode("Category", isRoot = true).apply { referenced = true }
        root.properties += property("name", string())
        root.properties += property("promoted", nullableRef(root), source)

        // When transforming...
        transformer.transform(root)

        // We expect one error, at the property:
        verify(exactly = 1) { logger.error(message("Category", "promoted"), source) }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `a required nullable reference to its own class in defs is reported`() {
        // Given Catalog.top: Node, where Node.promoted: Node?, required:
        val source = mockk<KSNode>(relaxed = true)
        val node = ObjectNode("Node").apply { referenced = true }
        node.properties += property("promoted", nullableRef(node), source)
        val root = ObjectNode("Catalog", isRoot = true).apply { properties += property("top", node) }

        // When transforming...
        transformer.transform(root)

        // We expect the recursive property reported, and not the one that only leads into the cycle:
        verify(exactly = 1) { logger.error(message("Node", "promoted"), source) }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `an optional reference to its own class is accepted`() {
        // Given Category.promoted: Category? = null:
        val root = ObjectNode("Category", isRoot = true).apply { referenced = true }
        root.properties += property("promoted", nullableRef(root), required = false)

        // When transforming...
        transformer.transform(root)

        // We expect no error:
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `a reference to its own class inside an array or a map is accepted`() {
        // Given Category.subcategories: List<Category> and Category.byName: Map<String, Category>, both required:
        val root = ObjectNode("Category", isRoot = true).apply { referenced = true }
        root.properties += property("subcategories", ArrayNode(RefNode(root)))
        root.properties += property("byName", MapNode(RefNode(root), mockk(relaxed = true)))

        // When transforming...
        transformer.transform(root)

        // We expect no error, since an array or a map can be empty:
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `a required nullable reference to a class that doesn't lead back is accepted`() {
        // Given Publication.author: Person and Publication.editor: Person?, both required:
        val person = ObjectNode("Person").apply { referenced = true; properties += property("name", string()) }
        val root = ObjectNode("Publication", isRoot = true).apply {
            properties += property("author", person)
            properties += property("editor", nullableRef(person))
        }

        // When transforming...
        transformer.transform(root)

        // We expect no error: the class is shared, not recursive.
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `a cycle of required properties through another class is reported at its nullable property`() {
        // Given Order.customer: Customer, required, and Customer.lastOrder: Order?, required:
        val back = mockk<KSNode>(relaxed = true)
        val root = ObjectNode("Order", isRoot = true).apply { referenced = true }
        val customer = ObjectNode("Customer").apply { properties += property("lastOrder", nullableRef(root), back) }
        root.properties += property("customer", customer)

        // When transforming...
        transformer.transform(root)

        // We expect the nullable property reported, since it's the one that can take a null default:
        verify(exactly = 1) { logger.error(message("Customer", "lastOrder"), back) }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `a cycle that goes through an optional property or an array is accepted`() {
        // Given Order.customer: Customer = ..., optional, and Customer.lastOrder: Order?, required:
        val order = ObjectNode("Order", isRoot = true).apply { referenced = true }
        order.properties += property(
            "customer",
            ObjectNode("Customer").apply { properties += property("lastOrder", nullableRef(order)) },
            required = false
        )

        // And Folder.files: List<File>, required, and File.shortcutTo: Folder?, required:
        val folder = ObjectNode("Folder", isRoot = true).apply { referenced = true }
        folder.properties += property(
            "files",
            ArrayNode(ObjectNode("File").apply { properties += property("shortcutTo", nullableRef(folder)) })
        )

        // When transforming each...
        transformer.transform(order)
        CompositeTransformer(RejectRequiredRecursiveReferences(dialectName, logger)).transform(folder)

        // We expect no error: the cycle isn't made of required properties only.
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `the tree is still copied`() {
        // Given a root with a reported property:
        val root = ObjectNode("Category", isRoot = true).apply { referenced = true }
        root.properties += property("promoted", nullableRef(root))

        // When transforming...
        val result = assertIs<ObjectNode>(transformer.transform(root))

        // We expect the copy, with its reference pointing at it, so processing goes on and reports any other error:
        assertSame(result, assertIs<RefNode>(result.properties.single().declaration).target)
    }
}
