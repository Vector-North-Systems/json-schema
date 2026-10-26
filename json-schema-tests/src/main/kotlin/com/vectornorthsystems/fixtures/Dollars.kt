package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot

/**
 * Runs of dollar signs, which would start a template if the generated string did not account for them.
 */
@SchemaRoot
data class Dollars(
    @SchemaDescription("price: $\${amount}, $\$\$, \${name} and \$name")
    val value: String,
)
