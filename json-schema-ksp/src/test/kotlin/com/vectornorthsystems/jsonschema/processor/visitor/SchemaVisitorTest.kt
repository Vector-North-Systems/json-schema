package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.doubles.*
import com.vectornorthsystems.jsonschema.processor.node.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.*

private const val PACKAGE = "com.vectornorthsystems.jsonschema"

/**
 * The visitor with a mocked [AnnotationProcessor], whose own tests cover the keywords it adds. Classes are described
 * with [struct], whose mocks the real [TypeClassifierImpl] classifies.
 */
class SchemaVisitorTest {

    private val logger = mockk<KSPLogger>(relaxed = true)
    private val annotationProcessor = mockk<AnnotationProcessor>(relaxed = true)

    // The class under test.
    private val visitor = SchemaVisitor(annotationProcessor, TypeClassifierImpl(), logger)

    /**
     * Extends [SchemaVisitor] to visit the declaration of [struct] as the root.
     */
    private fun SchemaVisitor.visitDeclarationOf(struct: Struct): ObjectNode {
        return assertIs<ObjectNode>(this.visitClassDeclaration(struct.declaration, data = ObjectNode("parent")))
    }

    private val ObjectNode.byName: Map<String, PropertyNode>
        get() = properties.associateBy { it.name }

    // region Objects

    @Test
    fun `the root is an object with its properties, in order`() {
        // Given a class with a string and an integer:
        val root = struct("Point") {
            property("label") { primitive(PrimitiveType.STRING) }
            property("x") { primitive(PrimitiveType.INTEGER) }
        }

        // When visiting it as the root...
        val node = visitor.visitDeclarationOf(root)

        // We expect an object named after the class, marked as the root, with both properties in declaration order:
        assertEquals("Point", node.name)
        assertTrue(node.isRoot)
        assertEquals(listOf("label", "x"), node.properties.map { it.name })
        assertEquals(PrimitiveType.STRING, assertIs<PrimitiveNode>(node.properties[0].declaration).type)
        assertEquals(PrimitiveType.INTEGER, assertIs<PrimitiveNode>(node.properties[1].declaration).type)
    }

    @Test
    fun `properties without a backing field or excluded are left out`() {
        // Given a class with a stored property, a computed one and an excluded one:
        val root = struct("Account") {
            property("email") { primitive(PrimitiveType.STRING) }
            property("domain", inBody = true, hasBackingField = false) { primitive(PrimitiveType.STRING) }
            property("password", annotations = listOf(ksAnnotation("$PACKAGE.SchemaExclude"))) {
                primitive(PrimitiveType.STRING)
            }
        }

        // When visiting it...
        val node = visitor.visitDeclarationOf(root)

        // We expect only the stored property:
        assertEquals(listOf("email"), node.properties.map { it.name })
    }

    @Test
    fun `constructor parameters without a default and marked properties are required`() {
        // Given a parameter without a default, one with a default, a body property, and a marked one with a default:
        val root = struct("Member") {
            property("id") { primitive(PrimitiveType.STRING) }
            property("name", hasDefault = true) { primitive(PrimitiveType.STRING) }
            property("joined", inBody = true) { primitive(PrimitiveType.INTEGER) }
            property("email", hasDefault = true, annotations = listOf(ksAnnotation("$PACKAGE.SchemaRequired"))) {
                primitive(PrimitiveType.STRING)
            }
        }

        // When visiting it...
        val node = visitor.visitDeclarationOf(root)

        // We expect the parameter without a default and the marked property to be required:
        assertEquals(
            mapOf("id" to true, "name" to false, "joined" to false, "email" to true),
            node.properties.associate { it.name to it.required }
        )
    }

    @Test
    fun `a class without a primary constructor has nothing required by default`() {
        // Given a class whose only property would be a parameter without a default, but without a primary constructor:
        val root = struct("Session") {
            property("user") { primitive(PrimitiveType.STRING) }
        }
        every { root.declaration.primaryConstructor } returns null

        // When visiting it...
        val node = visitor.visitDeclarationOf(root)

        // We expect the property, not required:
        assertFalse(node.properties.single().required)
    }

    @Test
    fun `each property goes through the annotation processor`() {
        // Given a class with a property:
        lateinit var name: KSPropertyDeclaration
        val root = struct("Event") {
            name = property("name") { primitive(PrimitiveType.STRING) }
        }

        // When visiting it...
        val node = visitor.visitDeclarationOf(root)

        // We expect the property node to get the descriptive annotations, and its type's node the others:
        val propertyNode = node.properties.single()
        verify {
            annotationProcessor.processDescriptiveAnnotations(name, propertyNode)
            annotationProcessor.processStringAnnotations(name, propertyNode.declaration)
            annotationProcessor.processIntegerAnnotations(name, propertyNode.declaration)
            annotationProcessor.processArrayAnnotations(name, propertyNode.declaration)
        }
    }

    @Test
    fun `property types are read as members of the class's type`() {
        // Given a generic class used with two type arguments, whose property is the type argument of each use:
        val box = struct("Box") {
            property("value", typeArgument = 0)
        }
        val root = struct("Shelf") {
            property("label") { box.type(primitive(PrimitiveType.STRING)) }
            property("count") { box.type(primitive(PrimitiveType.INTEGER)) }
        }

        // When visiting the class that uses both...
        val node = visitor.visitDeclarationOf(root)

        // We expect two separate objects, not a reference, each with the type argument of its use:
        val label = assertIs<ObjectNode>(node.byName.getValue("label").declaration)
        val count = assertIs<ObjectNode>(node.byName.getValue("count").declaration)
        assertEquals(PrimitiveType.STRING, assertIs<PrimitiveNode>(label.properties.single().declaration).type)
        assertEquals(PrimitiveType.INTEGER, assertIs<PrimitiveNode>(count.properties.single().declaration).type)
        assertFalse(label.referenced)
        assertFalse(count.referenced)
    }

    // endregion

    // region Shapes

    @Test
    fun `a primitive keeps the minimum of its shape`() {
        // Given an unsigned integer, which starts at zero:
        val root = struct("Measurements") {
            property("level") { kotlin("UInt") }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect an integer with a minimum of zero:
        val level = assertIs<PrimitiveNode>(node.properties.single().declaration)
        assertEquals(PrimitiveType.INTEGER, level.type)
        assertEquals(0.0, level.minimum)
    }

    @Test
    fun `a primitive array is an array of its primitive`() {
        // Given an IntArray:
        val root = struct("Measurements") {
            property("samples") { kotlin("IntArray") }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect an array of integers:
        val samples = assertIs<ArrayNode>(node.properties.single().declaration)
        assertEquals(PrimitiveType.INTEGER, assertIs<PrimitiveNode>(samples.declaration).type)
    }

    @Test
    fun `an enum is a string with its entries`() {
        // Given an enum:
        val root = struct("Recipe") {
            property("unitSystem") { enum("METRIC", "IMPERIAL") }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect a string limited to the entries:
        val unitSystem = assertIs<PrimitiveNode>(node.properties.single().declaration)
        assertEquals(PrimitiveType.STRING, unitSystem.type)
        assertEquals(listOf("METRIC", "IMPERIAL"), unitSystem.enum)
    }

    @Test
    fun `a collection is an array of its element`() {
        // Given a list of nullable strings:
        val root = struct("Profile") {
            property("tags") { list(primitive(PrimitiveType.STRING, nullable = true)) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect an array of nullable strings, the array itself not nullable:
        val tags = assertIs<ArrayNode>(node.properties.single().declaration)
        assertFalse(tags.nullable)
        assertTrue(assertIs<PrimitiveNode>(tags.declaration).nullable)
    }

    @Test
    fun `a map with string keys is a map of its values`() {
        // Given a map from strings to integers:
        val root = struct("Additional") {
            property("series") { map(primitive(PrimitiveType.STRING), primitive(PrimitiveType.INTEGER)) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect a map of integers, and no errors:
        val series = assertIs<MapNode>(node.properties.single().declaration)
        assertEquals(PrimitiveType.INTEGER, assertIs<PrimitiveNode>(series.declaration).type)
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `a map with keys other than strings is an error`() {
        // Given a map from integers to strings:
        lateinit var labels: KSPropertyDeclaration
        val root = struct("Additional") {
            labels = property("labels") { map(primitive(PrimitiveType.INTEGER), primitive(PrimitiveType.STRING)) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect an error at the property, and the map still built, so other errors are reported too:
        verify { logger.error(match { it.startsWith("Map keys must be String") }, labels) }
        assertIs<MapNode>(node.properties.single().declaration)
    }

    @Test
    fun `a value class is the schema of the type it wraps, nullable if used as nullable`() {
        // Given a value class wrapping a string, used as is and as nullable:
        val root = struct("Subscriber") {
            property("id") { value("UserId", primitive(PrimitiveType.STRING)) }
            property("previous") { value("UserId", primitive(PrimitiveType.STRING), nullable = true) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect a string for both, nullable only where the value class is:
        val id = assertIs<PrimitiveNode>(node.byName.getValue("id").declaration)
        val previous = assertIs<PrimitiveNode>(node.byName.getValue("previous").declaration)
        assertEquals(PrimitiveType.STRING, id.type)
        assertFalse(id.nullable)
        assertEquals(PrimitiveType.STRING, previous.type)
        assertTrue(previous.nullable)
    }

    @Test
    fun `an alias is the schema of its target, nullable if either is`() {
        // Given an alias, a nullable use of it, and an alias of a nullable type:
        val root = struct("Aliases") {
            property("plain") { alias("com.example.Name", primitive(PrimitiveType.STRING)) }
            property("nullableUse") { alias("com.example.Name", primitive(PrimitiveType.STRING), nullable = true) }
            property("nullableTarget") {
                alias("com.example.MaybeName", primitive(PrimitiveType.STRING, nullable = true))
            }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect a string for each, nullable unless neither the alias nor its use is:
        assertEquals(
            mapOf("plain" to false, "nullableUse" to true, "nullableTarget" to true),
            node.properties.associate { it.name to assertIs<PrimitiveNode>(it.declaration).nullable }
        )
    }

    @Test
    fun `an unsupported type is an error with a placeholder`() {
        // Given a property of a type that can't be described:
        lateinit var anything: KSPropertyDeclaration
        val root = struct("Bag") {
            anything = property("anything") { kotlin("Any") }
            property("name") { primitive(PrimitiveType.STRING) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect an error at the property, a string in its place, and the next property still visited:
        verify { logger.error("No schema for this property: Any could be anything; use a concrete type", anything) }
        assertEquals(PrimitiveType.STRING, assertIs<PrimitiveNode>(node.byName.getValue("anything").declaration).type)
        assertEquals(listOf("anything", "name"), node.properties.map { it.name })
    }

    @Test
    fun `nodes other than classes are left as they are, with a warning`() {
        // Given a node the visitor doesn't handle, such as a file:
        val file = mockk<KSFile>(relaxed = true)
        val data = ObjectNode("data")

        // When visiting it...
        val node: Node = visitor.visitFile(file, data)

        // We expect the data back, and a warning:
        assertSame(data, node)
        verify { logger.warn(match { it.startsWith("Unhandled node type") }, null) }
    }

    // endregion

    // region References

    @Test
    fun `a root that contains itself is referenced`() {
        // Given a tree node whose children are tree nodes:
        val root = struct("TreeNode") {
            property("children") { list(self()) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect the children to reference the root itself, which keeps its name since it isn't put in $defs:
        val children = assertIs<ArrayNode>(node.properties.single().declaration)
        assertSame(node, assertIs<RefNode>(children.declaration).target)
        assertTrue(node.referenced)
        assertEquals("TreeNode", node.defName)
    }

    @Test
    fun `a nested class that contains itself is referenced from inside`() {
        // Given an organization whose head is an employee, who has a nullable manager that is an employee:
        val employee = struct("Employee") {
            property("manager", hasDefault = true) { self(nullable = true) }
        }
        val root = struct("Organization") {
            property("head") { employee.type() }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect the employee built once, and the manager a nullable reference to it, named for $defs:
        val head = assertIs<ObjectNode>(node.properties.single().declaration)
        val manager = assertIs<RefNode>(head.properties.single().declaration)
        assertSame(head, manager.target)
        assertTrue(manager.nullable)
        assertTrue(head.referenced)
        assertEquals("Employee", head.defName)
        assertFalse(head.isRoot)
    }

    @Test
    fun `a class used twice is built once and referenced the second time`() {
        // Given a customer with two addresses:
        val address = struct("Address") {
            property("city") { primitive(PrimitiveType.STRING) }
        }
        val root = struct("Customer") {
            property("billing") { address.type() }
            property("shipping") { address.type() }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect the address built for the first, and the second to reference it:
        val billing = assertIs<ObjectNode>(node.byName.getValue("billing").declaration)
        val shipping = assertIs<RefNode>(node.byName.getValue("shipping").declaration)
        assertSame(billing, shipping.target)
        assertTrue(billing.referenced)
    }

    @Test
    fun `nullability of a use doesn't make the class a different one`() {
        // Given a resident with a home, and a nullable previous home of the same class:
        val residence = struct("Residence") {
            property("city") { primitive(PrimitiveType.STRING) }
        }
        val root = struct("Resident") {
            property("home") { residence.type() }
            property("previous", hasDefault = true) { residence.type(nullable = true) }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect a single residence, referenced by a nullable reference, the object itself not nullable:
        val home = assertIs<ObjectNode>(node.byName.getValue("home").declaration)
        val previous = assertIs<RefNode>(node.byName.getValue("previous").declaration)
        assertSame(home, previous.target)
        assertTrue(previous.nullable)
        assertFalse(home.nullable)
    }

    @Test
    fun `classes with the same simple name get distinct names in defs`() {
        // Given two classes named Address in different packages, each used twice:
        val first = struct("Address", "com.example.billing.Address")
        val second = struct("Address", "com.example.shipping.Address")
        val root = struct("Order") {
            property("billing") { first.type() }
            property("billingCopy") { first.type() }
            property("shipping") { second.type() }
            property("shippingCopy") { second.type() }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect two separate objects, the second one's name with a suffix:
        val billing = assertIs<ObjectNode>(node.byName.getValue("billing").declaration)
        val shipping = assertIs<ObjectNode>(node.byName.getValue("shipping").declaration)
        assertEquals("Address", billing.defName)
        assertEquals("Address2", shipping.defName)
    }

    @Test
    fun `objects nested deeper than the limit are an error with a placeholder`() {
        // Given a chain of 40 classes, each holding the next, as generics can nest without repeating a type:
        val levels = (0 until 40).map { struct("Level$it") }
        levels.zipWithNext { level, next -> level.extend { property("next") { next.type() } } }

        // When visiting the first...
        val node = visitor.visitDeclarationOf(levels.first())

        // We expect 32 objects, the root included, and a string in place of the 33rd, which is reported:
        var current: Node = node
        repeat(32) { current = assertIs<ObjectNode>(current).properties.single().declaration }
        assertEquals(PrimitiveType.STRING, assertIs<PrimitiveNode>(current).type)
        verify(exactly = 1) {
            logger.error(match { it.endsWith("is nested more than 32 objects deep") }, levels[32].declaration)
        }
    }

    @Test
    fun `an object that isn't referenced keeps its name and isn't marked`() {
        // Given a recipe with a single quantity:
        val root = struct("Recipe") {
            property("quantity") {
                struct("Quantity") {
                    property("amount") { primitive(PrimitiveType.NUMBER) }
                }
            }
        }

        // When visiting...
        val node = visitor.visitDeclarationOf(root)

        // We expect the quantity inline, not referenced:
        val built = assertIs<ObjectNode>(node.properties.single().declaration)
        assertFalse(built.referenced)
        assertFalse(node.referenced)
    }

    // endregion
}
