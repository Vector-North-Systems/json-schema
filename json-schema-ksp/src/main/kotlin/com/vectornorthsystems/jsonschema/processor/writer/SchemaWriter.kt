package com.vectornorthsystems.jsonschema.processor.writer

import com.vectornorthsystems.jsonschema.processor.node.ArrayNode
import com.vectornorthsystems.jsonschema.processor.node.MapNode
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.node.PrimitiveNode
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode
import com.vectornorthsystems.jsonschema.processor.node.RefNode
import kotlin.math.abs
import kotlin.math.round

/**
 * The JSON schema for this context.
 */
internal val Node.schema: String
    get() = SchemaWriter().write(this)

/**
 * Writes a JSON schema, collecting the recursive objects it meets into `$defs`.
 */
private class SchemaWriter {
    private val builder = StringBuilder()

    /** Recursive objects referenced so far, in the order they're met. */
    private val defs = mutableListOf<ObjectNode>()

    /**
     * Writes the schema from a node.
     * @param root The node.
     */
    fun write(root: Node): String {
        builder.append("{")
        writeSchema(root)

        if (defs.isNotEmpty()) {
            builder.append(",\"${Structure.DEFS}\":{")
            // A definition can reference further definitions, which are appended while iterating.
            var index = 0
            while (index < defs.size) {
                val def = defs[index]
                if (index > 0) builder.append(",")
                builder.append("${def.defName.toJsonString()}:{")
                // The definition itself is never null, only the places referencing it may be.
                writeObject(def, nullable = false)
                builder.append("}")
                index++
            }
            builder.append("}")
        }
        builder.append("}")
        return builder.toString()
    }

    /**
     * Writes the fields of the JSON schema object for [node], without its enclosing braces,
     * so the caller decides which object the fields belong to.
     */
    private fun writeSchema(node: Node) {
        when (node) {
            is ArrayNode -> {
                writeType(Types.ARRAY, node.nullable)
                if (node.prefixItems.isNotEmpty()) {
                    builder.append(",\"${ArrayProperties.PREFIX_ITEMS}\":[")
                    node.prefixItems.forEachIndexed { index, item ->
                        if (index > 0) builder.append(",")
                        builder.append("{")
                        writeSchema(item)
                        builder.append("}")
                    }
                    builder.append("]")
                }
                builder.append(",\"${ArrayProperties.ITEMS}\":{")
                writeSchema(node.declaration)
                builder.append("}")
                node.minItems?.let { builder.append(",\"${ArrayProperties.MIN_ITEMS}\":$it") }
                node.maxItems?.let { builder.append(",\"${ArrayProperties.MAX_ITEMS}\":$it") }
            }

            is ObjectNode -> {
                // A recursive object is defined once in $defs, so even its first occurrence is a reference.
                if (node.referenced && !node.isRoot) {
                    writeRef(node, node.nullable)
                } else {
                    writeObject(node, node.nullable)
                }
            }

            is RefNode -> writeRef(node.target, node.nullable)

            is MapNode -> {
                writeType(Types.OBJECT, node.nullable)
                builder.append(",\"${ObjectProperties.ADDITIONAL_PROPERTIES}\":{")
                writeSchema(node.declaration)
                builder.append("}")
            }

            is PrimitiveNode -> {
                writeType(node.type.keyword, node.nullable)
                node.enum?.let { values ->
                    builder.append(",\"${StringProperties.ENUM}\":")
                    builder.append(values.joinToString(",", "[", "]") { it.toJsonString() })
                }
                node.format?.let { builder.append(",\"${StringProperties.FORMAT}\":${it.toJsonString()}") }
                node.minimum?.let { builder.append(",\"${NumberProperties.MINIMUM}\":${it.toJsonNumber()}") }
                node.maximum?.let { builder.append(",\"${NumberProperties.MAXIMUM}\":${it.toJsonNumber()}") }
            }

            is PropertyNode -> {
                node.title?.let {
                    builder.append("\"${Description.TITLE}\":${it.toJsonString()},")
                }
                node.description?.let {
                    builder.append("\"${Description.DESCRIPTION}\":${it.toJsonString()},")
                }
                writeSchema(node.declaration)
            }
        }
    }

    private fun writeObject(context: ObjectNode, nullable: Boolean) {
        writeType(Types.OBJECT, nullable)
        builder.append(",\"${ObjectProperties.PROPERTIES}\":{")
        context.properties.forEachIndexed { index, property ->
            if (index > 0) builder.append(",")
            builder.append("${property.name.toJsonString()}:{")
            writeSchema(property)
            builder.append("}")
        }
        builder.append("}")

        val required = context.properties.filter { it.required }
        if (required.isNotEmpty()) {
            builder.append(",\"${ObjectProperties.REQUIRED}\":")
            builder.append(required.joinToString(",", "[", "]") { it.name.toJsonString() })
        }
    }

    private fun writeRef(target: ObjectNode, nullable: Boolean) {
        if (!target.isRoot && target !in defs) defs.add(target)
        val ref = "\"${Structure.REF}\":${target.ref.toJsonString()}"
        // A reference has no type of its own to add "null" to.
        if (nullable) {
            builder.append("\"${Structure.ANY_OF}\":[{$ref},{\"${Structure.TYPE}\":\"${Types.NULL}\"}]")
        } else {
            builder.append(ref)
        }
    }

    private fun writeType(type: String, nullable: Boolean) {
        if (nullable) {
            builder.append("\"${Structure.TYPE}\":[\"$type\",\"${Types.NULL}\"]")
        } else {
            builder.append("\"${Structure.TYPE}\":\"$type\"")
        }
    }
}

/**
 * Where references to this object point.
 */
private val ObjectNode.ref: String
    get() = if (isRoot) "#" else "#/${Structure.DEFS}/$defName"

private fun String.toJsonString(): String = buildString {
    append('"')
    for (char in this@toJsonString) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> if (char < ' ') {
                append("\\u")
                append(char.code.toString(16).padStart(4, '0'))
            } else {
                append(char)
            }
        }
    }
    append('"')
}

/**
 * Whole numbers without a fraction, so an integer property gets `"minimum":1` rather than `"minimum":1.0`.
 */
private fun Double.toJsonNumber(): String =
    if (this == round(this) && abs(this) < 1e15) toLong().toString() else toString()
