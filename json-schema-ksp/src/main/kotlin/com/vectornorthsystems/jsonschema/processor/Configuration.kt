package com.vectornorthsystems.jsonschema.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.vectornorthsystems.jsonschema.SchemaTarget

/**
 * The prefix of this processor's KSP options, e.g. `jsonSchema.targets`. Options without it belong to other processors.
 */
internal const val JSON_SCHEMA_PREFIX = "jsonSchema."

/**
 * The dialects generated for every [com.vectornorthsystems.jsonschema.SchemaRoot] class in the module, unless the
 * class sets its own.
 */
internal const val DEFAULT_TARGETS_OPTION = "targets"

/**
 * The options this processor reads, without the prefix.
 */
internal val KNOWN_OPTION_KEYS = listOf(DEFAULT_TARGETS_OPTION)

/**
 * The dialects as written in the KSP options, in lower case.
 */
internal val validTargets = SchemaTarget.entries.map { it.name.lowercase() }

internal class ConfigurationProcessor(
    val logger: KSPLogger
) {

    /**
     * Builds a configuration object from the environment input.
     *
     * @param options The environment supplied KSP options.
     */
    fun process(options: Map<String, String>): Configuration {
        val config = MutableConfiguration()
        options.entries.filter { (key, _) -> isProcessorOption(key) }
            .map { (key, value) -> toOption(key, value) }
            .forEach { (key, value) -> processOption(key, value, config) }

        return config.asImmutable()
    }

    /**
     * Given an option prefixed with [JSON_SCHEMA_PREFIX], removes the prefix and returns the option.
     */
    private fun toOption(key: String, value: String): Pair<String, String> = key.trim().removePrefix(JSON_SCHEMA_PREFIX) to value

    /**
     * Returns options for this processor, i.e.: options that starts with [JSON_SCHEMA_PREFIX]
     */
    private fun isProcessorOption(key: String): Boolean = key.trim().startsWith(JSON_SCHEMA_PREFIX)

    fun processOption(key: String, value: String, config: MutableConfiguration) {
        when (key) {
            DEFAULT_TARGETS_OPTION -> parseTargetDialect(value, config)
            // Ignored, but likely a typo: the user meant to configure something.
            else -> logger.warn("Unknown option '$JSON_SCHEMA_PREFIX$key' is ignored. Valid options are: ${KNOWN_OPTION_KEYS.map { JSON_SCHEMA_PREFIX + it }}")
        }
    }

    /**
     * Parses a comma separated list of targets, ignoring case and blanks around each one, e.g. `"gemini, openai"`.
     * The parsed targets replace the defaults, so the option narrows them down.
     * Logs an error, and keeps the defaults, if the list is empty or has an unknown target: the error fails the build
     * anyway, so the defaults only let the round finish, reporting any other errors too.
     */
    private fun parseTargetDialect(value: String, config: MutableConfiguration) {
        val names = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (names.isEmpty()) {
            logger.error("No targets set in '$JSON_SCHEMA_PREFIX$DEFAULT_TARGETS_OPTION'. Valid targets are: $validTargets")
            return
        }

        val unknown = names.filter { name -> name.lowercase() !in validTargets }
        if (unknown.isNotEmpty()) {
            logger.error("Unknown targets $unknown in '$JSON_SCHEMA_PREFIX$DEFAULT_TARGETS_OPTION'. Valid targets are: $validTargets")
            return
        }

        config.dialects = names.map { name -> SchemaTarget.valueOf(name.uppercase()) }.distinct().toMutableList()
    }
}

internal data class MutableConfiguration(
    var dialects: MutableList<SchemaTarget> = SchemaTarget.entries.toMutableList(),
) {

    fun asImmutable(): Configuration {
        return Configuration(
            dialects = dialects.toSet(),
        )
    }
}

/**
 * A configuration, obtained from processing KSP options.
 */
internal data class Configuration(
    val dialects: Set<SchemaTarget>,
)
