package com.vectornorthsystems.jsonschema.processor.doubles

import com.google.devtools.ksp.symbol.*
import io.mockk.every
import io.mockk.mockk


/**
 * Builds and returns a mock [KSName].
 */
fun ksName(value: String): KSName = mockk { every { asString() } returns value }

/**
 * Builds and returns a mock [KSDeclaration].
 */
fun ksDeclaration(qualifiedName: String?): KSDeclaration = mockk {
    every { this@mockk.qualifiedName } returns qualifiedName?.let { ksName(it) }
}

/**
 * Builds and returns a mock [KSClassDeclaration].
 */
fun ksClassDeclaration(
    kind: ClassKind = ClassKind.CLASS,
    modifiers: Set<Modifier> = emptySet(),
    typeParameters: List<KSTypeParameter> = emptyList(),
    simpleName: String = "Test",
    qualifiedName: String = "com.example.$simpleName",
    declarations: List<KSDeclaration> = emptyList(),
    properties: List<KSPropertyDeclaration> = emptyList(),
): KSClassDeclaration = mockk {
    every { classKind } returns kind
    every { this@mockk.modifiers } returns modifiers
    every { this@mockk.typeParameters } returns typeParameters
    every { this@mockk.simpleName } returns ksName(simpleName)
    every { this@mockk.qualifiedName } returns ksName(qualifiedName)
    every { this@mockk.declarations } returns declarations.asSequence()
    every { getAllProperties() } returns properties.asSequence()
}

/**
 * Builds and returns a mock [KSTypeAlias] that stands for [target].
 */
fun ksTypeAlias(qualifiedName: String, target: KSType): KSTypeAlias = mockk {
    every { this@mockk.qualifiedName } returns ksName(qualifiedName)
    every { type.resolve() } returns target
}

/**
 * Builds and returns a mock [KSTypeParameter], which has no qualified name.
 */
fun ksTypeParameter(parameterName: String): KSTypeParameter = mockk {
    every { qualifiedName } returns null
    every { name } returns ksName(parameterName)
}

/**
 * Builds and returns a mock [KSAnnotation], with the given [arguments] by name.
 */
fun ksAnnotation(qualifiedName: String, arguments: Map<String, Any?> = emptyMap()): KSAnnotation = mockk {
    every { shortName } returns ksName(qualifiedName.substringAfterLast('.'))
    every { annotationType.resolve().declaration } returns ksDeclaration(qualifiedName)
    every { this@mockk.arguments } returns arguments.map { (argumentName, argumentValue) ->
        mockk<KSValueArgument> {
            every { name } returns ksName(argumentName)
            every { value } returns argumentValue
        }
    }
}

/**
 * Builds and returns a mock [KSType].
 */
fun ksType(qualifiedName: String, vararg arguments: KSType?, nullable: Boolean = false): KSType =
    ksType(ksDeclaration(qualifiedName), *arguments, nullable = nullable)

/**
 * Builds and returns a mock [KSType] of the given [declaration].
 */
fun ksType(declaration: KSDeclaration, vararg arguments: KSType?, nullable: Boolean = false): KSType = mockk {
    every { this@mockk.declaration } returns declaration
    every { isMarkedNullable } returns nullable
    every { makeNotNullable() } returns this@mockk
    every { this@mockk.arguments } returns arguments.map { argument ->
        mockk<KSTypeArgument> {
            every { type } returns argument?.let { resolved -> mockk { every { resolve() } returns resolved } }
        }
    }
}

/**
 * Builds and returns a mock [KSPropertyDeclaration].
 */
fun ksProperty(seed: List<KSAnnotation>): KSPropertyDeclaration = mockk {
    every { annotations } returns seed.asSequence()
}

/**
 * Builds and returns a mock [KSPropertyDeclaration] that is [memberType] when seen as a member of any type.
 */
fun ksPropertyOf(memberType: KSType, hasBackingField: Boolean = true): KSPropertyDeclaration = mockk {
    every { this@mockk.hasBackingField } returns hasBackingField
    every { asMemberOf(any()) } returns memberType
}