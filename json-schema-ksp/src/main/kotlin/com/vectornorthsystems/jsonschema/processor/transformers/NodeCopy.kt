package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.symbol.KSNode
import com.vectornorthsystems.jsonschema.processor.PrimitiveType
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.node.PrimitiveNode
import com.vectornorthsystems.jsonschema.processor.node.PropertyNode

/**
 * Copies the fields every [Node] has from [original].
 */
internal fun <T : Node> T.copyCommon(original: Node): T = apply {
    title = original.title
    description = original.description
    nullable = original.nullable
}

/**
 * Copies a [PropertyNode] node with default values as the current node.
 * @return A new node.
 */
internal fun PropertyNode.copy(
    name: String = this.name,
    declaration: Node = this.declaration,
    source: KSNode = this.source,
    required: Boolean = this.required
) = PropertyNode(
    name = name,
    declaration = declaration,
    source = source,
    required = required
).also {
    it.wrapDescribedReference = this.wrapDescribedReference
}.copyCommon(this)

/**
 * Copies a primitive node.
 */
internal fun PrimitiveNode.copy(
    type: PrimitiveType = this.type,
    enum: List<String>? = this.enum,
) = PrimitiveNode(
    type = type,
    enum = enum,
).also {
    it.format = this.format
    it.minimum = this.minimum
    it.maximum = this.maximum
}.copyCommon(this)

/**
 * Copies an object node.
 */
internal fun ObjectNode.copy(
    name: String = this.name,
    isRoot: Boolean = this.isRoot,
    additionalProperties: Boolean? = this.additionalProperties
) = ObjectNode(
    name = name,
    isRoot = isRoot,
    additionalProperties = additionalProperties
).also {
    it.referenced = this.referenced
    it.defName = this.defName
}.copyCommon(this)