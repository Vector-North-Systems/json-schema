package com.vectornorthsystems.jsonschema.processor.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.vectornorthsystems.jsonschema.SchemaTarget

/**
 * The schema of one dialect, lowered and written, ready to become a constant of the generated object.
 *
 * @property target The dialect, which names the constant.
 * @property json The schema, as written by [com.vectornorthsystems.jsonschema.processor.writer.schema].
 */
internal data class TargetSchema(
    val target: SchemaTarget,
    val json: String,
)

/**
 * Generates the Kotlin object that holds the schemas of a [com.vectornorthsystems.jsonschema.SchemaRoot] class: one
 * constant per dialect, plus `get(target)` to pick one at runtime.
 *
 * It only assembles and writes code. Choosing the dialects, lowering the tree and writing the JSON happen before, so
 * the schemas arrive as plain strings.
 */
internal interface SchemaObjectGenerator {

    /**
     * Writes the object for [classDeclaration] to its own file, named after the object, in the class's package.
     *
     * @param classDeclaration The [com.vectornorthsystems.jsonschema.SchemaRoot] class the schemas describe.
     * @param objectName The name of the generated object, already validated.
     * @param schemas The schema of each dialect to generate, in the order of the constants.
     */
    fun generate(classDeclaration: KSClassDeclaration, objectName: String, schemas: List<TargetSchema>)
}
