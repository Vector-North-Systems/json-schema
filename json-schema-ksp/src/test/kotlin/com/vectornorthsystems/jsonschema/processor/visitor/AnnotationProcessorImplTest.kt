package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.vectornorthsystems.jsonschema.SchemaFormat
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.doubles.ksAnnotation
import com.vectornorthsystems.jsonschema.processor.doubles.ksProperty
import com.vectornorthsystems.jsonschema.processor.node.ArrayNode
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.node.PrimitiveNode
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val PACKAGE = "com.vectornorthsystems.jsonschema"

private fun schemaAnnotation(name: String, vararg arguments: Pair<String, Any?>) =
    ksAnnotation("$PACKAGE.$name", mapOf(*arguments))

/**
 * An enum entry, which KSP gives as a [KSType] whose declaration is named after the entry.
 */
private fun enumEntry(name: String): KSType = mockk { every { declaration.simpleName.asString() } returns name }

class AnnotationProcessorImplTest {

    internal lateinit var logger: KSPLogger
    internal lateinit var processor: AnnotationProcessorImpl

    @BeforeTest
    fun before() {
        logger = mockk(relaxed = true)
        processor = AnnotationProcessorImpl(logger)
    }

    private fun propertyNode() = PropertyNode("test", PrimitiveNode(PrimitiveType.STRING), mockk(relaxed = true), false)

    private fun arrayNode() = ArrayNode(PrimitiveNode(PrimitiveType.STRING))

    private fun formatAnnotation(entry: String) = schemaAnnotation("SchemaFormat", "format" to enumEntry(entry))

    private fun unannotated(): KSPropertyDeclaration = ksProperty(
        annotations = listOf(ksAnnotation("kotlinx.serialization.Serializable"), ksAnnotation("kotlin.jvm.JvmInline"))
    )

    private fun verifyNoErrors() = verify(exactly = 0) { logger.error(any(), any()) }

    // region processDescriptiveAnnotations

    @Test
    fun `properties without title and description add no such values to the node`() {
        // Given a property declaration without title or description:
        val property = unannotated()

        // And the supplied node:
        val node = propertyNode()

        // When processing...
        processor.processDescriptiveAnnotations(property, node)

        // We expect nothing to be written to the node:
        assertNull(node.title)
        assertNull(node.description)
    }

    @Test
    fun `properties with a title and description write them to the node`() {
        // Given a property declaration with a title and a description:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaTitle", "text" to "A title"),
                schemaAnnotation("SchemaDescription", "text" to "A description"),
            )
        )

        // And the supplied node:
        val node = propertyNode()

        // When processing...
        processor.processDescriptiveAnnotations(property, node)

        // We expect both values to be written to the node:
        assertEquals("A title", node.title)
        assertEquals("A description", node.description)
    }

    @Test
    fun `a title without a description only writes the title`() {
        // Given a property declaration with a title only:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaTitle", "text" to "A title")))

        // And the supplied node:
        val node = propertyNode()

        // When processing...
        processor.processDescriptiveAnnotations(property, node)

        // We expect the title to be written, and the description to stay empty:
        assertEquals("A title", node.title)
        assertNull(node.description)
    }

    // endregion

    // region processStringAnnotations

    @Test
    fun `format on a non-primitive property is an error`() {
        // Given a property declaration with a format:
        val property = ksProperty(annotations = listOf(formatAnnotation("DATE")))

        // When processing it against an object node...
        processor.processStringAnnotations(property, ObjectNode("Test"))

        // We expect the error to be logged against the property:
        verify { logger.error("@SchemaFormat only applies to String properties", property) }
    }

    @Test
    fun `format on a non-string primitive is an error and leaves the node untouched`() {
        // Given a property declaration with a format:
        val property = ksProperty(annotations = listOf(formatAnnotation("DATE")))

        // And an integer node:
        val node = PrimitiveNode(PrimitiveType.INTEGER)

        // When processing...
        processor.processStringAnnotations(property, node)

        // We expect the error to be logged, and no format written:
        verify { logger.error("@SchemaFormat only applies to String properties", property) }
        assertNull(node.format)
    }

    @Test
    fun `format on an enum is an error and leaves the node untouched`() {
        // Given a property declaration with a format:
        val property = ksProperty(annotations = listOf(formatAnnotation("DATE")))

        // And a string node that is an enum:
        val node = PrimitiveNode(PrimitiveType.STRING, enum = listOf("A", "B"))

        // When processing...
        processor.processStringAnnotations(property, node)

        // We expect the error to be logged, and no format written:
        verify { logger.error("@SchemaFormat only applies to String properties", property) }
        assertNull(node.format)
    }

    @Test
    fun `string properties without format are left untouched`() {
        // Given a property declaration without a format:
        val property = unannotated()

        // And a string node:
        val node = PrimitiveNode(PrimitiveType.STRING)

        // When processing...
        processor.processStringAnnotations(property, node)

        // We expect no format and no errors:
        assertNull(node.format)
        verifyNoErrors()
    }

    @Test
    fun `format on a string property is written to the node`() {
        SchemaFormat.Format.entries.forEach { format ->
            // Given a property declaration with each supported format:
            val property = ksProperty(annotations = listOf(formatAnnotation(format.name)))

            // And a string node:
            val node = PrimitiveNode(PrimitiveType.STRING)

            // When processing...
            processor.processStringAnnotations(property, node)

            // We expect the format's JSON schema value to be written to the node:
            assertEquals(format.value, node.format)
        }

        // And no errors:
        verifyNoErrors()
    }

    // endregion

    // region processIntegerAnnotations

    @Test
    fun `a non-finite minimum is an error`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { minimum ->
            // Given a property declaration with a minimum that is not a finite number:
            val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMinimum", "value" to minimum)))

            // When processing it against an integer node...
            processor.processIntegerAnnotations(property, PrimitiveNode(PrimitiveType.INTEGER))

            // We expect the error to be logged:
            verify { logger.error("@SchemaMinimum must be finite, found $minimum", property) }
        }
    }

    @Test
    fun `a non-finite maximum is an error`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { maximum ->
            // Given a property declaration with a maximum that is not a finite number:
            val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMaximum", "value" to maximum)))

            // When processing it against an integer node...
            processor.processIntegerAnnotations(property, PrimitiveNode(PrimitiveType.INTEGER))

            // We expect the error to be logged:
            verify { logger.error("@SchemaMaximum must be finite, found $maximum", property) }
        }
    }

    @Test
    fun `limits on a non-number property are an error and leave the node untouched`() {
        // Given a property declaration with a minimum and a maximum:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinimum", "value" to 1.0),
                schemaAnnotation("SchemaMaximum", "value" to 5.0),
            )
        )

        // And a string node:
        val node = PrimitiveNode(PrimitiveType.STRING)

        // When processing...
        processor.processIntegerAnnotations(property, node)

        // We expect the error to be logged, and no limits written:
        verify { logger.error("@SchemaMinimum and @SchemaMaximum only apply to number properties", property) }
        assertNull(node.minimum)
        assertNull(node.maximum)
    }

    @Test
    fun `limits on a non-primitive property are an error`() {
        // Given a property declaration with a minimum:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMinimum", "value" to 1.0)))

        // When processing it against an object node...
        processor.processIntegerAnnotations(property, ObjectNode("Test"))

        // We expect the error to be logged:
        verify { logger.error("@SchemaMinimum and @SchemaMaximum only apply to number properties", property) }
    }

    @Test
    fun `a minimum greater than the maximum is an error`() {
        // Given a property declaration whose minimum is above its maximum:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinimum", "value" to 10.0),
                schemaAnnotation("SchemaMaximum", "value" to 5.0),
            )
        )

        // When processing it against an integer node...
        processor.processIntegerAnnotations(property, PrimitiveNode(PrimitiveType.INTEGER))

        // We expect the error to be logged:
        verify { logger.error("@SchemaMinimum(10.0) is greater than @SchemaMaximum(5.0)", property) }
    }

    @Test
    fun `integer properties without limits are left untouched`() {
        // Given a property declaration without limits:
        val property = unannotated()

        // And an integer node:
        val node = PrimitiveNode(PrimitiveType.INTEGER)

        // When processing...
        processor.processIntegerAnnotations(property, node)

        // We expect no limits and no errors:
        assertNull(node.minimum)
        assertNull(node.maximum)
        verifyNoErrors()
    }

    @Test
    fun `limits on integer and number properties are written to the node`() {
        listOf(PrimitiveType.INTEGER, PrimitiveType.NUMBER).forEach { type ->
            // Given a property declaration with a minimum and a maximum:
            val property = ksProperty(
                annotations = listOf(
                    schemaAnnotation("SchemaMinimum", "value" to 1.0),
                    schemaAnnotation("SchemaMaximum", "value" to 5.0),
                )
            )

            // And a node of each number type:
            val node = PrimitiveNode(type)

            // When processing...
            processor.processIntegerAnnotations(property, node)

            // We expect both limits to be written to the node:
            assertEquals(1.0, node.minimum)
            assertEquals(5.0, node.maximum)
        }

        // And no errors:
        verifyNoErrors()
    }

    @Test
    fun `a minimum alone leaves the maximum untouched`() {
        // Given a property declaration with a minimum only:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMinimum", "value" to 1.0)))

        // And an integer node:
        val node = PrimitiveNode(PrimitiveType.INTEGER)

        // When processing...
        processor.processIntegerAnnotations(property, node)

        // We expect only the minimum to be written, and no errors:
        assertEquals(1.0, node.minimum)
        assertNull(node.maximum)
        verifyNoErrors()
    }

    @Test
    fun `a maximum alone leaves the minimum untouched`() {
        // Given a property declaration with a maximum only:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMaximum", "value" to 5.0)))

        // And an integer node:
        val node = PrimitiveNode(PrimitiveType.INTEGER)

        // When processing...
        processor.processIntegerAnnotations(property, node)

        // We expect only the maximum to be written, and no errors:
        assertNull(node.minimum)
        assertEquals(5.0, node.maximum)
        verifyNoErrors()
    }

    @Test
    fun `limits override the minimum of unsigned types`() {
        // Given a property declaration with a negative minimum:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMinimum", "value" to -5.0)))

        // And an integer node that starts at 0, like unsigned types do:
        val node = PrimitiveNode(PrimitiveType.INTEGER).apply { minimum = 0.0 }

        // When processing...
        processor.processIntegerAnnotations(property, node)

        // We expect the annotation to replace the starting minimum, with no errors:
        assertEquals(-5.0, node.minimum)
        verifyNoErrors()
    }

    @Test
    fun `equal limits are valid`() {
        // Given a property declaration whose minimum equals its maximum:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinimum", "value" to 3.0),
                schemaAnnotation("SchemaMaximum", "value" to 3.0),
            )
        )

        // When processing it against an integer node...
        processor.processIntegerAnnotations(property, PrimitiveNode(PrimitiveType.INTEGER))

        // We expect no errors:
        verifyNoErrors()
    }

    // endregion

    // region processArrayAnnotations

    @Test
    fun `item limits on a non-array property are an error`() {
        // Given a property declaration with a minimum and a maximum number of items:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinItems", "value" to 1),
                schemaAnnotation("SchemaMaxItems", "value" to 5),
            )
        )

        // When processing it against a string node...
        processor.processArrayAnnotations(property, PrimitiveNode(PrimitiveType.STRING))

        // We expect the error to be logged:
        verify {
            logger.error(
                "@SchemaMinItems and @SchemaMaxItems only apply to collection properties",
                property
            )
        }
    }

    @Test
    fun `a negative minimum number of items is an error`() {
        // Given a property declaration with a negative minimum number of items:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMinItems", "value" to -1)))

        // When processing it against an array node...
        processor.processArrayAnnotations(property, arrayNode())

        // We expect the error to be logged:
        verify { logger.error("@SchemaMinItems(-1) can't be negative", property) }
    }

    @Test
    fun `a negative maximum number of items is an error`() {
        // Given a property declaration with a negative maximum number of items:
        val property = ksProperty(annotations = listOf(schemaAnnotation("SchemaMaxItems", "value" to -1)))

        // When processing it against an array node...
        processor.processArrayAnnotations(property, arrayNode())

        // We expect the error to be logged:
        verify { logger.error("@SchemaMaxItems(-1) can't be negative", property) }
    }

    @Test
    fun `a minimum number of items greater than the maximum is an error`() {
        // Given a property declaration whose minimum number of items is above its maximum:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinItems", "value" to 5),
                schemaAnnotation("SchemaMaxItems", "value" to 2),
            )
        )

        // When processing it against an array node...
        processor.processArrayAnnotations(property, arrayNode())

        // We expect the error to be logged:
        verify { logger.error("@SchemaMinItems(5) is greater than @SchemaMaxItems(2)", property) }
    }

    @Test
    fun `array properties without annotations are left untouched`() {
        // Given a property declaration without item annotations:
        val property = unannotated()

        // And an array node:
        val node = arrayNode()

        // When processing, with a schema resolver that fails if used...
        processor.processArrayAnnotations(property, node)

        // We expect nothing to be written to the node, and no errors:
        assertNull(node.minItems)
        assertNull(node.maxItems)
        verifyNoErrors()
    }

    @Test
    fun `item limits are written to the array node`() {
        // Given a property declaration with a minimum and a maximum number of items:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinItems", "value" to 1),
                schemaAnnotation("SchemaMaxItems", "value" to 5),
            )
        )

        // And an array node:
        val node = arrayNode()

        // When processing...
        processor.processArrayAnnotations(property, node)

        // We expect both limits to be written, with no errors:
        assertEquals(1, node.minItems)
        assertEquals(5, node.maxItems)
        verifyNoErrors()
    }

    @Test
    fun `zero items is a valid limit`() {
        // Given a property declaration whose minimum and maximum number of items are both zero:
        val property = ksProperty(
            annotations = listOf(
                schemaAnnotation("SchemaMinItems", "value" to 0),
                schemaAnnotation("SchemaMaxItems", "value" to 0),
            )
        )

        // When processing it against an array node...
        processor.processArrayAnnotations(property, arrayNode())

        // We expect no errors:
        verifyNoErrors()
    }

    // endregion
}
