package com.vectornorthsystems.jsonschema.processor.visitor

import com.google.devtools.ksp.symbol.KSType

/**
 * Decides what kind of schema a type produces, without building it.
 */
internal interface TypeClassifier {
    /**
     * Returns the [TypeShape] of [type].
     */
    fun classify(type: KSType): TypeShape
}