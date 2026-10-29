package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.generator.SchemaObjectGenerator
import com.vectornorthsystems.jsonschema.processor.generator.TargetSchema
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.transformers.transformer
import com.vectornorthsystems.jsonschema.processor.visitor.*
import com.vectornorthsystems.jsonschema.processor.writer.schema

/**
 * A processor that converts Kotlin data classes annotated with [com.vectornorthsystems.jsonschema.SchemaRoot] into a schema representation.
 * This processor traverses the class structure, capturing properties and their types to generate a schema.
 *
 * Each root goes through the same steps: visit the class into a tree, choose its dialects, lower the tree and write the
 * JSON for each one, and generate the object that holds them. This class only orders the steps, and checks the roots
 * and the names of their objects.
 *
 * @param schemaObjectGenerator Generates the object holding the schemas.
 * @param configuration The configuration (from the KSP environment)
 * @param logger The logger.
 */
internal class SchemaProcessor(
    private val schemaObjectGenerator: SchemaObjectGenerator,
    private val configuration: Configuration,
    private val logger: KSPLogger,
) : SymbolProcessor {

    /**
     * The generated objects, by qualified name, to report clashes instead of failing to create the file.
     */
    private val generated = mutableMapOf<String, KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Symbols that reference types not resolved yet are deferred to the next round.
        val (valid, deferred) = resolver.getSymbolsWithAnnotation(getQualifiedName<SchemaRoot>())
            .partition { it.validate() }

        valid.filterIsInstance<KSClassDeclaration>()
            .filter { symbol -> symbol.isValidRoot(logger) }
            .forEach { symbol ->
                // We parse the tree with all the supported richness
                val visitor = SchemaVisitor(AnnotationProcessorImpl(logger), TypeClassifierImpl(), logger)
                val objectName = objectName(symbol) ?: return@forEach
                // The root has no parent, so it's its own placeholder.
                val parent = ObjectNode(symbol.simpleName.asString())
                // Call the Schema visitor to build the tree.
                val root = symbol.accept(visitor, parent)

                // Lower the tree and write the JSON for each dialect: the class's own, or the module's. Transformers
                // copy the tree, so one tree serves every dialect.
                // Sorted, so the generated constants keep the order of SchemaTarget dialects.
                val schemas = symbol.dialects(logger)
                    .orDefaults(configuration.dialects)
                    .sortedBy { target -> target.ordinal }
                    .map { target -> TargetSchema(target, target.transformer().transform(root).schema) }

                schemaObjectGenerator.generate(symbol, objectName, schemas)
            }

        return deferred
    }

    /**
     * The name of the generated object: [SchemaRoot.name], or `<ClassName>Schema`.
     * Logs an error and returns null if it isn't a valid identifier or is already generated in the package.
     */
    private fun objectName(classDeclaration: KSClassDeclaration): String? {
        val custom = classDeclaration.annotation<SchemaRoot>()?.argument("name") as? String
        val name = custom?.ifEmpty { null } ?: "${classDeclaration.simpleName.asString()}Schema"

        if (!name.isIdentifier()) {
            logger.error(
                "@SchemaRoot(name = \"$name\") isn't a valid Kotlin identifier",
                classDeclaration
            )
            return null
        }

        val qualifiedName = "${classDeclaration.packageName.asString()}.$name"
        val previous = generated.putIfAbsent(qualifiedName, classDeclaration)
        if (previous != null) {
            logger.error(
                "$name is already generated for ${previous.qualifiedName?.asString()}, " +
                        "set a different @SchemaRoot(name)",
                classDeclaration
            )
            return null
        }

        return name
    }

    /**
     * These dialects, or [defaultDialects] if there are none.
     */
    private fun Set<SchemaTarget>.orDefaults(defaultDialects: Set<SchemaTarget>): Set<SchemaTarget> {
        if (this.isNotEmpty()) {
            return this
        }

        return defaultDialects
    }
}
