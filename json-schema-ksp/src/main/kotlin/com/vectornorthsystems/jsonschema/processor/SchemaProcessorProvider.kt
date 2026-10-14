package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * A provider for the CodeToSchemaProcessor.
 * Called by the KSP framework.
 */
class SchemaProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SchemaProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}