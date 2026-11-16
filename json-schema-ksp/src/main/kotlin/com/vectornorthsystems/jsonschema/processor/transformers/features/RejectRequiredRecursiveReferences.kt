package com.vectornorthsystems.jsonschema.processor.transformers.features

import com.google.devtools.ksp.processing.KSPLogger
import com.vectornorthsystems.jsonschema.processor.node.*
import com.vectornorthsystems.jsonschema.processor.transformers.BaseTransformerImpl
import java.util.IdentityHashMap

/**
 * Breaks the build if a required, nullable property leads back to its own class through required properties only, for
 * the dialects that reject it:
 *
 * ```kotlin
 * data class Category(val name: String, val promoted: Category?)
 * ```
 *
 * Gemini rejects the schema of this class, with "Invalid JSON payload: syntax error in request body", whether the
 * class is the root (`"$ref": "#"`) or in `$defs`. It accepts it when the property is optional (`= null`), and when
 * the way back goes through an array (`List<Category>`), which can be empty.
 *
 * Only the nullable properties of such a cycle are reported: they're the ones that can take a `null` default.
 *
 * @param dialectName The dialect that rejects them, for the message.
 * @param logger Reports the properties found, which fails the build.
 */
internal class RejectRequiredRecursiveReferences(val dialectName: String, val logger: KSPLogger) :
    BaseTransformerImpl() {

    /**
     * The objects each object leads to through a required property of that type, by the property. Properties that hold
     * the object in an array or a map aren't included.
     */
    private val requiredEdges = IdentityHashMap<ObjectNode, MutableList<Pair<PropertyNode, ObjectNode>>>()

    override fun transform(root: Node): Node {
        collect(root)

        requiredEdges.forEach { (owner, edges) ->
            edges.filter { (property, target) -> property.declaration.nullable && target.leadsTo(owner) }
                .forEach { (property, _) ->
                    logger.error(
                        "A required property that leads back to its own class, ${owner.name}, isn't supported by " +
                                "the $dialectName dialect. Make it optional with a default: " +
                                "`${property.name}: ... = null`.",
                        property.source
                    )
                }
        }

        return super.transform(root)
    }

    /**
     * Walks the tree, collecting the [requiredEdges] of every object. References aren't followed: their target is
     * written somewhere else in the tree.
     */
    private fun collect(node: Node) {
        when (node) {
            is ObjectNode -> {
                val edges = requiredEdges.getOrPut(node) { mutableListOf() }
                node.properties.forEach { property ->
                    val target = when (val declaration = property.declaration) {
                        is ObjectNode -> declaration
                        is RefNode -> declaration.target
                        else -> null
                    }
                    if (property.required && target != null) {
                        edges += property to target
                    }
                    collect(property.declaration)
                }
            }

            is ArrayNode -> collect(node.declaration)
            is MapNode -> collect(node.declaration)
            is PropertyNode -> collect(node.declaration)
            is PrimitiveNode, is RefNode -> Unit
        }
    }

    /**
     * Whether [destination] can be reached from this object through required properties.
     */
    private fun ObjectNode.leadsTo(destination: ObjectNode): Boolean {
        val seen = IdentityHashMap<ObjectNode, Unit>()
        val pending = ArrayDeque(listOf(this))
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            if (current === destination) return true
            if (seen.put(current, Unit) == null) {
                requiredEdges[current]?.forEach { (_, target) -> pending += target }
            }
        }

        return false
    }
}
