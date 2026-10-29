package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.vectornorthsystems.jsonschema.processor.generator.SchemaObjectGeneratorImpl

/**
 * A provider for the CodeToSchemaProcessor.
 * Called by the KSP framework.
 */
class SchemaProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val config = ConfigurationProcessor(environment.logger)
            .process(environment.options)

        return SchemaProcessor(
            schemaObjectGenerator = SchemaObjectGeneratorImpl(environment.codeGenerator, environment.logger),
            configuration = config,
            logger = environment.logger
        )
    }
}