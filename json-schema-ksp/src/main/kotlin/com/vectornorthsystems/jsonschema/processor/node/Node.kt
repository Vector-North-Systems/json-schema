package com.vectornorthsystems.jsonschema.processor.node

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType

/**
 * A node in the object hierarchy.
 */
internal sealed class Node {
    /**
     * The title, if any.
     */
    var title: String? = null

    /**
     * The description, if any.
     */
    var description: String? = null

    /**
     * Whether the value may be `null`, for nullable Kotlin types.
     */
    var nullable: Boolean = false
}

/**
 * An object node.
 * @property name The class's simple name.
 * @property isRoot Whether this is the [com.vectornorthsystems.jsonschema.SchemaRoot] object, which references point to with `#`.
 */
internal class ObjectNode(
    val name: String,
    var isRoot: Boolean = false
) : Node() {
    /**
     * The properties of this object type.
     */
    val properties: MutableList<PropertyNode> = mutableListOf()

    /**
     * Whether a property refers back to this object, so it's written once and referenced with `$ref`.
     */
    var referenced: Boolean = false

    /**
     * The name of this object in `$defs`, unique within the schema.
     */
    var defName: String = name
}

/**
 * A reference to an object that contains itself, e.g. the children of a tree node.
 * @property target The object referred to, whose properties are written where it's defined.
 */
internal class RefNode(val target: ObjectNode) : Node()

/**
 * An array node.
 * @property declaration A Primitive or Object declaration
 */
internal class ArrayNode(val declaration: Node) : Node() {
    /**
     * The minimum number of items, if specified. See [com.vectornorthsystems.jsonschema.SchemaMinItems]
     */
    var minItems: Int? = null

    /**
     * The maximum number of items, if specified. See [com.vectornorthsystems.jsonschema.SchemaMaxItems]
     */
    var maxItems: Int? = null
}

/**
 * A map node, an object with arbitrary keys.
 * @property declaration The schema of the map's values.
 */
internal class MapNode(val declaration: Node) : Node()

/**
 * A primitive node.
 * @property type The JSON schema type: "integer", "number", "boolean" or "string".
 * @property enum The allowed values, for enum classes.
 */
internal class PrimitiveNode(
    val type: PrimitiveType,
    val enum: List<String>? = null
) : Node() {
    /**
     * The string syntax, see [com.vectornorthsystems.jsonschema.SchemaFormat].
     */
    var format: String? = null

    /**
     * The minimum inclusive value of a number property. See [com.vectornorthsystems.jsonschema.SchemaMinimum]
     */
    var minimum: Double? = null

    /**
     * The maximum inclusive value of a number property. See [com.vectornorthsystems.jsonschema.SchemaMaximum]
     */
    var maximum: Double? = null
}

/**
 * A named property of an object.
 *
 * @property name The name of the property, as declared.
 * @property declaration The schema node of the property's type.
 * @property source The source of the property, for error reporting.
 * @property required Whether the AI model must include the property: it's a constructor parameter without a default, or
 * marked [com.vectornorthsystems.jsonschema.SchemaRequired].
 */
internal class PropertyNode(
    val name: String,
    val declaration: Node,
    val source: KSNode,
    val required: Boolean
) : Node()
