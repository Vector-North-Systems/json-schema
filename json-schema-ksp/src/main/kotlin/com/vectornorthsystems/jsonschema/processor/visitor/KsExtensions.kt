package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.vectornorthsystems.jsonschema.SchemaExclude
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget

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
    val entryName = enumEntryName(argument(name)) ?: return null

    return enumValues<E>().firstOrNull { it.name == entryName }
}

/**
 * The name of an enum entry given as an argument value, which KSP gives as the entry's [KSType] or
 * [KSClassDeclaration] depending on the version. Null if the value isn't an enum entry.
 */
internal fun enumEntryName(value: Any?): String? = when (value) {
    is KSType -> value.declaration.simpleName.asString()
    is KSDeclaration -> value.simpleName.asString()
    else -> null
}

/**
 * Obtains the targets set in [SchemaRoot.targets], or empty if none are set.
 * Logs an error, pointing at the class, for a target this processor doesn't know, since the class wouldn't get the
 * schemas it asked for.
 */
internal fun KSClassDeclaration.dialects(logger: KSPLogger): Set<SchemaTarget> {
    // KSP documents array arguments as an Array, but KSP 2.3.10 gives a List (of KSClassDeclaration enum entries), as
    // the Targets fixture showed. Both are accepted, in case it differs between versions. Something else can't be read,
    // which would silently drop the class's dialects, so it's an error.
    val entries = when (val value = annotation<SchemaRoot>()?.argument("targets")) {
        is List<*> -> value
        is Array<*> -> value.toList()
        null -> return emptySet()
        else -> {
            logger.error("@SchemaRoot(targets) can't be read from a ${value::class.simpleName}", this)
            return emptySet()
        }
    }

    return entries.mapNotNull { entry ->
        val entryName = enumEntryName(entry)
        val target = SchemaTarget.entries.firstOrNull { it.name == entryName }
        if (target == null) {
            logger.error(
                "@SchemaRoot(targets) has an unknown target '$entryName'. Valid targets are: ${SchemaTarget.entries}",
                this
            )
        }
        target
    }.toSet()
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