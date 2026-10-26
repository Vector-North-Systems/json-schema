package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.processor.node.Node
import com.vectornorthsystems.jsonschema.processor.node.ObjectNode
import com.vectornorthsystems.jsonschema.processor.transformers.GeminiTransformer
import com.vectornorthsystems.jsonschema.processor.visitor.AnnotationProcessorImpl
import com.vectornorthsystems.jsonschema.processor.visitor.SchemaVisitor
import com.vectornorthsystems.jsonschema.processor.visitor.TypeClassifierImpl
import com.vectornorthsystems.jsonschema.processor.visitor.annotation
import com.vectornorthsystems.jsonschema.processor.visitor.argument
import com.vectornorthsystems.jsonschema.processor.visitor.isValidRoot
import com.vectornorthsystems.jsonschema.processor.writer.schema
import java.io.OutputStream


private const val INTERNAL_WITH_SPACE = "internal "
private const val EMPTY = ""

/**
 * A processor that converts Kotlin data classes annotated with [com.vectornorthsystems.jsonschema.SchemaRoot] into a schema representation.
 * This processor traverses the class structure, capturing properties and their types to generate a schema.
 */
internal class SchemaProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    /**
     * The generated functions, by qualified function name, to report clashes instead of failing to create the file.
     */
    private val generated = mutableMapOf<String, KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Symbols that reference types not resolved yet are deferred to the next round.
        val (valid, deferred) = resolver.getSymbolsWithAnnotation(getQualifiedName<SchemaRoot>())
            .partition { it.validate() }

        valid.filterIsInstance<KSClassDeclaration>()
            .forEach { symbol ->
                if (!symbol.isValidRoot(logger)) {
                    return@forEach
                }
                val visitor = SchemaVisitor(AnnotationProcessorImpl(logger), TypeClassifierImpl(), logger)
                val functionName = functionName(symbol) ?: return@forEach
                // The root has no parent, so it's its own placeholder.
                val parent = ObjectNode(symbol.simpleName.asString())
                // Call the Schema visitor to build the tree.
                val root = symbol.accept(visitor, parent)
                // Lower the tree for the target.
                val schemaTree = GeminiTransformer().transform(root)
                // Generate the schema
                generateSchema(schemaTree, codeGenerator, symbol, functionName)
            }

        return deferred
    }

    /**
     * The name of the generated function: [SchemaRoot.functionName], or `get<ClassName>Schema`.
     * Logs an error and returns null if it isn't a valid identifier or is already generated in the package.
     */
    private fun functionName(classDeclaration: KSClassDeclaration): String? {
        val custom = classDeclaration.annotation<SchemaRoot>()?.argument("functionName") as? String
        val functionName = custom?.ifEmpty { null } ?: "get${classDeclaration.simpleName.asString()}Schema"

        if (!functionName.isIdentifier()) {
            logger.error(
                "@SchemaRoot(functionName = \"$functionName\") isn't a valid Kotlin identifier",
                classDeclaration
            )
            return null
        }

        val qualifiedName = "${classDeclaration.packageName.asString()}.$functionName"
        val previous = generated.putIfAbsent(qualifiedName, classDeclaration)
        if (previous != null) {
            logger.error(
                "$functionName() is already generated for ${previous.qualifiedName?.asString()}, " +
                        "set a different @SchemaRoot(functionName)",
                classDeclaration
            )
            return null
        }

        return functionName
    }

    /**
     * Writes a function to the generated file that returns a schema string.
     *
     * The function signature is `functionName(): String`, in the package of [classDeclaration] and a file named after
     * the function.
     *
     * @param rootNode The root context of the generation.
     * @param codeGenerator The KSP code generator.
     * @param classDeclaration The starting declaration.
     * @param functionName The name of the generated function.
     */
    private fun generateSchema(
        rootNode: Node,
        codeGenerator: CodeGenerator,
        classDeclaration: KSClassDeclaration,
        functionName: String,
    ) {
        logger.info("Generating schema for root context: $rootNode")
        // Replace `$`'s that would start a template in the raw string.
        val schema = rootNode.schema.replace("$", $$"${'$'}")

        generateNewFile(codeGenerator, classDeclaration, functionName).use { stream ->
            val className = classDeclaration.simpleName.asString()
            val packageName = classDeclaration.packageName.asString()
            val qualifiedName = classDeclaration.qualifiedName?.asString() ?: className

            // Classes in the root package have no package directive.
            if (packageName.isNotEmpty()) {
                stream.write("package $packageName\n\n")
            }

            // The function lives in its own file, so a private class gets an internal function: a private one
            // couldn't be called from anywhere.
            val visibilityModifier = when {
                classDeclaration.isInternal() || classDeclaration.isPrivate() -> INTERNAL_WITH_SPACE
                else -> EMPTY // Public is used by default
            }

            stream.write(
                """
                    /**
                     * Generated function for [$className].
                     *
                     * @see [$qualifiedName]
                     */
                    ${visibilityModifier}fun $functionName(): String {
                           return ""${'"'}$schema""${'"'}
                    }
                """.trimIndent()
            )
        }
    }

    private fun generateNewFile(
        codeGenerator: CodeGenerator,
        classDeclaration: KSClassDeclaration,
        functionName: String,
    ): OutputStream {
        return codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, classDeclaration.containingFile!!),
            packageName = classDeclaration.packageName.asString(),
            extensionName = "kt",
            fileName = functionName.replaceFirstChar { it.uppercase() }
        )
    }

    /**
     * Writes a string to the OutputStream (From the KSP documentation!)
     */
    private fun OutputStream.write(string: String): Unit {
        this.write(string.toByteArray())
    }
}

