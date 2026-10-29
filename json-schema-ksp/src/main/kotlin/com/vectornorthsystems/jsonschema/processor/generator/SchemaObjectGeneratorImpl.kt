package com.vectornorthsystems.jsonschema.processor.generator

import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.getQualifiedName

/**
 * Generates the object with KSP's [CodeGenerator].
 *
 * [generate] reads what it needs off the class and writes the file; [source] builds the code from plain values, so it
 * can be checked without KSP.
 */
internal class SchemaObjectGeneratorImpl(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SchemaObjectGenerator {

    override fun generate(classDeclaration: KSClassDeclaration, objectName: String, schemas: List<TargetSchema>) {
        logger.info("Generating $objectName with the schemas: ${schemas.map { it.target }}")

        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val source = source(
            packageName = packageName,
            className = className,
            qualifiedName = classDeclaration.qualifiedName?.asString() ?: className,
            // The object lives in its own file, so a private class gets an internal object: a private one couldn't
            // be used from anywhere.
            internal = classDeclaration.isInternal() || classDeclaration.isPrivate(),
            objectName = objectName,
            schemas = schemas,
        )

        codeGenerator.createNewFile(
            // Only the class's own file affects the object, so KSP can skip it when other files change.
            dependencies = Dependencies(aggregating = false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = objectName.replaceFirstChar { it.uppercase() },
            extensionName = "kt",
        ).use { stream -> stream.write(source.toByteArray()) }
    }

    /**
     * The Kotlin source of the object.
     *
     * @param packageName The package of the object, empty for the root package.
     * @param className The simple name of the class the schemas describe, for the KDoc.
     * @param qualifiedName The qualified name of that class, for the KDoc's `@see`.
     * @param internal Whether the object is internal, or public otherwise.
     * @param objectName The name of the generated object.
     * @param schemas The schema of each dialect, in the order of the constants.
     */
    fun source(
        packageName: String,
        className: String,
        qualifiedName: String,
        internal: Boolean,
        objectName: String,
        schemas: List<TargetSchema>,
    ): String = buildString {
        // Classes in the root package have no package directive.
        if (packageName.isNotEmpty()) {
            appendLine("package $packageName")
            appendLine()
        }
        appendLine("import ${getQualifiedName<SchemaTarget>()}")
        appendLine()

        appendLine("/**")
        appendLine(" * Generated schemas for [$className], one per dialect.")
        appendLine(" *")
        appendLine(" * @see [$qualifiedName]")
        appendLine(" */")
        appendLine("${if (internal) "internal " else ""}object $objectName {")

        // One constant per dialect. A raw string keeps the JSON as it is: its quotes and backslashes need no escaping,
        // and JSON never has three quotes in a row, which would end the string. Only `$` would start a template.
        schemas.forEach { (target, json) ->
            appendLine("    /**")
            appendLine("     * The schema for the ${target.name} dialect.")
            appendLine("     */")
            appendLine("    const val ${target.name}: String = \"\"\"${json.escapeTemplates()}\"\"\"")
            appendLine()
        }

        // Only the generated dialects get a branch, and the others give null, instead of the `when` not compiling.
        // With every dialect generated there's no `else`, so the `when` stays exhaustive.
        appendLine("    /**")
        appendLine("     * The schema of [target], or null if it isn't generated for [$className].")
        appendLine("     */")
        appendLine("    fun getOrNull(target: SchemaTarget): String? = when (target) {")
        schemas.forEach { (target, _) ->
            appendLine("        SchemaTarget.${target.name} -> ${target.name}")
        }
        if (schemas.map { it.target }.toSet() != SchemaTarget.entries.toSet()) {
            appendLine("        else -> null")
        }
        appendLine("    }")
        appendLine()

        // A missing dialect is a build setting to change, so the message says which ones exist and where to add it.
        // In this `$$` string, `$$name` is filled in now, while `$name` is left as a template for the generated code.
        val generated = schemas.joinToString { it.target.name }.ifEmpty { "none" }
        appendLine("    /**")
        appendLine("     * The schema of [target].")
        appendLine("     *")
        appendLine("     * @throws IllegalArgumentException If [target] isn't generated for [$className].")
        appendLine("     */")
        appendLine("    operator fun get(target: SchemaTarget): String = getOrNull(target)")
        appendLine("        ?: throw IllegalArgumentException(")
        appendLine($$"            \"$$objectName has no $target schema, only $$generated. Add ${target.name.lowercase()} to the \" +")
        appendLine($$"                \"jsonSchema.targets KSP option, or $target to @SchemaRoot(targets = ...) on $$className.\"")
        appendLine("        )")
        appendLine("}")
    }

    /**
     * Replaces the `$`s that would start a template in the raw string.
     */
    private fun String.escapeTemplates(): String = replace("$", $$"${'$'}")
}
