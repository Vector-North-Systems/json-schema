package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Modifier
import com.vectornorthsystems.jsonschema.processor.PrimitiveType

internal class TypeClassifierImpl : TypeClassifier {

    override fun classify(type: KSType): TypeShape {
        val declaration = type.declaration
        val qualifiedName = declaration.qualifiedName?.asString()

        return when {
            // Resolve type aliases
            declaration is KSTypeAlias -> TypeShape.Alias(declaration.type.resolve())

            declaration is KSTypeParameter -> TypeShape.Unsupported(
                "The type parameter ${declaration.name.asString()} has no type argument to describe; use a concrete type."
            )

            // Primitive types including arrays
            qualifiedName in knownIntegerTypes -> TypeShape.Primitive(PrimitiveType.INTEGER)
            qualifiedName in knownUnsignedTypes -> TypeShape.Primitive(PrimitiveType.INTEGER, minimum = 0.0)
            qualifiedName in knownNumberTypes -> TypeShape.Primitive(PrimitiveType.NUMBER)
            qualifiedName in knownBooleanTypes -> TypeShape.Primitive(PrimitiveType.BOOLEAN)
            qualifiedName in knownStringTypes -> TypeShape.Primitive(PrimitiveType.STRING)
            qualifiedName in knownPrimitiveArrayTypes -> TypeShape.PrimitiveCollection(qualifiedName!!)

            // Enum support
            declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS ->
                TypeShape.EnumOf(declaration.enumEntries())

            // Array support
            declaration.isArray() -> type.arguments.firstOrNull()?.type?.resolve()
                ?.let { TypeShape.Collection(it) }
                ?: TypeShape.Unsupported("cannot resolve the element type of $type")

            // Additional Properties support
            // A map is an object whose keys aren't known up front, so its values are described by additionalProperties.
            declaration.isMap() -> {
                val keyType = type.arguments.getOrNull(0)?.type?.resolve()
                val valueType = type.arguments.getOrNull(1)?.type?.resolve()

                if (keyType == null || valueType == null) {
                    TypeShape.Unsupported("cannot resolve the key and value types of $type")
                } else {
                    TypeShape.MapOf(key = keyType, value = valueType)
                }
            }

            qualifiedName == "kotlin.Any" -> TypeShape.Unsupported("Any could be anything; use a concrete type")

            declaration !is KSClassDeclaration -> TypeShape.Unsupported("$type isn't a class")

            // TODO: Describe sealed classes and interfaces with anyOf.
            declaration.classKind == ClassKind.INTERFACE || Modifier.SEALED in declaration.modifiers ||
                    Modifier.ABSTRACT in declaration.modifiers ->
                TypeShape.Unsupported("interfaces, sealed and abstract classes like $type aren't supported yet")

            else -> TypeShape.Object(declaration)
        }
    }
}
