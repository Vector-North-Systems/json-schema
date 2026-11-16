package com.vectornorthsystems.jsonschema.processor.transformers

import com.google.devtools.ksp.processing.KSPLogger
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.jsonschema.processor.transformers.features.RejectMaps
import com.vectornorthsystems.jsonschema.processor.transformers.features.RejectRequiredRecursiveReferences
import com.vectornorthsystems.jsonschema.processor.transformers.features.RequireAllProperties
import com.vectornorthsystems.jsonschema.processor.transformers.features.SetAdditionalPropertiesFalse
import com.vectornorthsystems.jsonschema.processor.transformers.features.WrapDescribedReferences

/**
 * The schemas Gemini accepts. The visitor's tree already fits them, so it's copied as is, except that a required
 * property that leads back to its own class is rejected: the API doesn't accept its schema.
 *
 * @param logger Reports those properties, which fails the build.
 */
internal fun geminiTransformer(logger: KSPLogger) = CompositeTransformer(
    RejectRequiredRecursiveReferences(dialectName = SchemaTarget.GEMINI.name, logger = logger)
)

/**
 * The schemas OpenAI accepts in strict mode: every property is required, no object allows additional properties, and
 * maps are rejected, since they are objects with arbitrary keys. A reference with a title or a description is wrapped
 * in `anyOf`: the API rejects the schema otherwise, with "$ref cannot have keywords {'description'}".
 *
 * @param logger Reports the maps found, which fails the build.
 */
internal fun openAiTransformer(logger: KSPLogger) = CompositeTransformer(
    RequireAllProperties(),
    SetAdditionalPropertiesFalse(),
    WrapDescribedReferences(),
    RejectMaps(dialectName = SchemaTarget.OPENAI.name, logger = logger)
)

/**
 * The transformer that lowers the tree for this dialect.
 *
 * A new dialect needs a branch here: the `when` has no `else`, so the compiler points at it when [SchemaTarget] grows.
 * Each call returns a new instance, since a transformer remembers the objects it copied and so transforms one tree.
 */
internal fun SchemaTarget.transformer(logger: KSPLogger): Transformer = when (this) {
    SchemaTarget.GEMINI -> geminiTransformer(logger)
    SchemaTarget.OPENAI -> openAiTransformer(logger)
}
