package com.vectornorthsystems.fixtures

import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget

// A class that sets its own dialects, overriding the module's jsonSchema.targets. Reading the argument from a real build
// is what showed that KSP gives it as a List, not the Array its docs describe: an argument that can't be read fails the
// build.
@SchemaRoot(targets = [SchemaTarget.GEMINI])
data class Planet(val name: String)
