# Kotlin JSON Schema Generator
[![Maven Central](https://img.shields.io/maven-central/v/com.vectornorthsystems/json-schema-annotations)](https://central.sonatype.com/artifact/com.vectornorthsystems/json-schema-annotations)
[![CI](https://github.com/Vector-North-Systems/json-schema/actions/workflows/ci.yml/badge.svg)](https://github.com/Vector-North-Systems/json-schema/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://vector-north-systems.github.io/json-schema/coverage.json)](https://github.com/Vector-North-Systems/json-schema/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE.txt)

Generates [JSON Schemas](https://json-schema.org/overview/what-is-jsonschema) from Kotlin classes at compile time,
using [KSP](https://kotlinlang.org/docs/ksp-overview.html). The schemas are produced in the dialect that each AI provider
accepts for structured output.

Annotate the class to obtain the schemas from:

```kotlin
@SchemaRoot
data class Recipe(
    val instructions: String,
    val ingredients: List<Ingredient>,
    val isColdDish: Boolean? = null
)
```

The build generates the schemas, one for each supported provider:

```kotlin
RecipeSchema.GEMINI   // {"type":"object","properties":{"instructions":{"type":"string"},...
RecipeSchema.OPENAI   // The same class, following OpenAI's rules.
```

Use one when requesting structured output from the corresponding provider, and decode the answer into the same class:

```kotlin
val recipe = Json.decodeFromString<Recipe>(answer)
```

## What it's for
Structured output helps you obtain a response from an AI provider in a predefined format, making it easier to decode.
This feature, supported by most providers, moves the burden of parsing and formatting the response to the provider's own
pipeline instead of the client. It works like this: in the request, we supply a JSON schema that describes the shape of
the response data.

The client's responsibility then is to keep the supplied schema in sync with the code, so decoding the response never fails.

With code already following the response shape, maintaining the corresponding schema is repetitive. To complicate things,
each AI provider accepts a different subset of JSON schema, with different rules, so one schema may not work everywhere.

The natural way to solve this is to generate the schemas from the classes:

- **Your classes are the single source of truth.** Change a class, and the next build regenerates its schemas.
- **Provider's rules are applied for you.** When a provider can’t express something in a class, the build fails and
says why, instead of the request.
- **No reflection and no runtime dependency.** Each schema is a string constant, built at compile time.

See [why this library](docs/why.md) exists to learn more.

## Supported dialects
| Provider      | `SchemaTarget` | Follows                                                                                                                                         | Guide                           |
|---------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| Google Gemini | `GEMINI`       | [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output#json-schema-support)                                         | [Gemini](docs/guides/gemini.md) |
| OpenAI        | `OPENAI`       | [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses#supported-schemas), strict mode | [OpenAI](docs/guides/openai.md) |

More providers are planned, Anthropic among them; see the [roadmap](docs/roadmap.md).

## Installation
Apply the [KSP plugin](https://github.com/google/ksp/releases) that matches your Kotlin version, then add the annotations
and the processor:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("com.vectornorthsystems:json-schema-annotations:0.4.0")
    ksp("com.vectornorthsystems:json-schema-ksp:0.4.0")
}
```

Both artifacts are on Maven Central. Requires JDK 17 or later. See [Getting started](docs/getting-started.md) for more
details and options.

## Usage
Following the example above:
```kotlin
@SchemaRoot
data class Recipe(
    val instructions: String,
    val ingredients: List<Ingredient>,
    val isColdDish: Boolean? = null
)
```
`@SchemaRoot` on `Recipe` generates a `RecipeSchema` object in the same package, with a constant per dialect:

```kotlin
object RecipeSchema {
    const val GEMINI: String = """{"type":"object","properties":{...},"required":["instructions","ingredients"]}"""
    const val OPENAI: String = """{"type":"object","properties":{...},"additionalProperties":false,"required":[...]}"""

    fun getOrNull(target: SchemaTarget): String? = ...
    operator fun get(target: SchemaTarget): String = ...
}
```

A schema is a string of JSON, ready to use in the request to the provider. The [Gemini](docs/guides/gemini.md) and
[OpenAI](docs/guides/openai.md) guides show where, with the whole request. To pick the provider at runtime, use
`RecipeSchema[target]`; see [Runtime targets](docs/guides/runtime-targets.md).

### The same class in each dialect
The schemas are minified; they're formatted here for readability. `RecipeSchema.GEMINI` is:

```json
{
  "type": "object",
  "properties": {
    "instructions": {"type": "string"},
    "ingredients": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {"type": "string"}
        },
        "required": ["name"]
      }
    },
    "isColdDish": {"type": ["boolean", "null"]}
  },
  "required": ["instructions", "ingredients"]
}
```

Note that `isColdDish` has a default, so it is not required and Gemini may leave it out. OpenAI's strict mode requires
every property and forbids additional ones, so `RecipeSchema.OPENAI` differs in these lines:

```diff
         "properties": {
           "name": {"type": "string"}
         },
+        "additionalProperties": false,
         "required": ["name"]
       }
     },
     "isColdDish": {"type": ["boolean", "null"]}
   },
-  "required": ["instructions", "ingredients"]
+  "additionalProperties": false,
+  "required": ["instructions", "ingredients", "isColdDish"]
 }
```

You don't write either by hand, and you don't need to keep track of the rules: they come from the class and the dialect.
See [Dialects](docs/reference/targets.md) for everything that differs.

### Descriptions and constraints
Structured output systems provide special properties to be used in the schema. These help guide the model or force an
output format on a field. For example, `"description"` describes a property, and `"minItems"` establishes the minimum
accepted items in a response array. To support this feature, this library provides annotations such as
`@SchemaDescription`, `@SchemaFormat`, `@SchemaMinimum`, and `@SchemaMaxItems`.
See [Annotations](docs/reference/annotations.md) for all of them.

## Documentation
| To                                                           | Read                                                             |
|--------------------------------------------------------------|------------------------------------------------------------------|
| Set up a project and generate a first schema                 | [Getting started](docs/getting-started.md)                       |
| Understand dialects, and required versus nullable properties | [Concepts](docs/concepts.md)                                     |
| Send a schema to a provider and decode the answer            | [Gemini](docs/guides/gemini.md), [OpenAI](docs/guides/openai.md) |
| Choose the provider at runtime                               | [Runtime targets](docs/guides/runtime-targets.md)                |
| Look up how a Kotlin type is described                       | [Types](docs/reference/types.md)                                 |
| Look up an annotation                                        | [Annotations](docs/reference/annotations.md)                     |
| Compare the dialects, or choose which ones are generated     | [Dialects](docs/reference/targets.md)                            |
| Fix a build error                                            | [Errors](docs/reference/errors.md)                               |
| See what's planned, and the known issues                     | [Roadmap](docs/roadmap.md)                                       |
| See the justification for this library                       | [Why this library](docs/why.md)                                  |

The schemas above are checked by `ReadmeTest` in `json-schema-tests`, and
[`samples`](json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples) has runnable round trips.

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md) and [CHANGELOG.md](CHANGELOG.md) for what changed in each version.
