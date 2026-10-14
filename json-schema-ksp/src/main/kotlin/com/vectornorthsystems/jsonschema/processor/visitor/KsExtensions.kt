package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.vectornorthsystems.jsonschema.SchemaExclude

/**
 * For details of the KSP symbol API, see https://kotlinlang.org/docs/ksp-kotlin-model.html
 */

/**
 * Validates a root.
 * Root schemas describe concrete objects, not polymorphic or enum declarations.
 */
fun KSClassDeclaration.isValidRoot(logger: KSPLogger?): Boolean {
    if (typeParameters.isNotEmpty()) {
        logger?.error("@SchemaRoot classes can't have type parameters", this)
        return false
    }
    if (classKind !in setOf(ClassKind.CLASS, ClassKind.OBJECT) ||
        Modifier.SEALED in modifiers || Modifier.ABSTRACT in modifiers
    ) {
        logger?.error(
            "@SchemaRoot requires a concrete class or object; enums, interfaces, annotation classes, sealed and abstract classes aren't supported",
            this
        )
        return false
    }
    return true
}

/**
 * Tells whether a property should not be excluded because is not marked with [SchemaExclude].
 */
internal val KSPropertyDeclaration.shouldNotBeExcluded: Boolean
    get() = annotation<SchemaExclude>() == null

/**
 * Produces a key that identifies a type with its type arguments, since `Box<String>` and `Box<Int>` have different schemas.
 */
internal fun KSType.key(): String = buildString {
    append(declaration.qualifiedName?.asString())

    if (arguments.isNotEmpty()) {
        arguments.joinTo(this, ",", "<", ">") { argument ->
            argument.type?.resolve()?.let { it.key() + if (it.isMarkedNullable) "?" else "" } ?: "*"
        }
    }
}

/**
 * The names of the primary constructor's parameters that have no default, which a decoder can only fill from the JSON.
 * Parameters are matched to properties by name, as decoders do, so this includes the ones passed on to a superclass.
 */
internal fun KSClassDeclaration.parametersWithoutDefault(): Set<String> =
    primaryConstructor?.parameters
        ?.filterNot { it.hasDefault }
        ?.mapNotNull { it.name?.asString() }
        ?.toSet()
        .orEmpty()

/**
 * Returns the string representation of enum constants.
 */
internal fun KSClassDeclaration.enumEntries(): List<String> =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.ENUM_ENTRY }
        .map { it.simpleName.asString() }
        .toList()

internal inline fun <reified T : Annotation> KSAnnotated.annotation(): KSAnnotation? =
    annotations.firstOrNull {
        it.shortName.asString() == T::class.simpleName &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == T::class.qualifiedName
    }

internal fun KSAnnotation.argument(name: String): Any? =
    arguments.firstOrNull { it.name?.asString() == name }?.value

/**
 * An enum argument, which KSP gives as the entry's [KSType] or [KSClassDeclaration] depending on the version.
 */
internal inline fun <reified E : Enum<E>> KSAnnotation.enumArgument(name: String): E? {
    val entry = when (val value = argument(name)) {
        is KSType -> value.declaration
        is KSDeclaration -> value
        else -> return null
    }

    return enumValues<E>().firstOrNull { it.name == entry.simpleName.asString() }
}

/**
 * Tells whether the declaration is a known Map type or not.
 * See [knownMapTypes]
 */
internal fun KSDeclaration.isMap(): Boolean {
    return this.qualifiedName?.asString() in knownMapTypes
}

/**
 * Tells whether the declaration is a known array type or not.
 * See [knownArrayTypes]
 */
internal fun KSDeclaration.isArray(): Boolean {
    return this.qualifiedName?.asString() in knownArrayTypes
}