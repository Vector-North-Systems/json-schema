# Getting started

This page sets up a project, generates a first schema and sends it in a first request.

## Requirements
- JDK 17 or later.
- The [KSP plugin](https://github.com/google/ksp/releases) that matches your Kotlin version.

The library is built and tested with Kotlin 2.4.20 and KSP 2.3.10.

## Setup
The library has two artifacts, both on Maven Central:

| Artifact                  | What it is                                              | Configuration    |
|---------------------------|---------------------------------------------------------|------------------|
| `json-schema-annotations` | The annotations you put on your classes                 | `implementation` |
| `json-schema-ksp`         | The processor that generates the schemas, at build time | `ksp`            |

### JVM and Android
```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("com.vectornorthsystems:json-schema-annotations:0.4.0")
    ksp("com.vectornorthsystems:json-schema-ksp:0.4.0")
}
```

### Kotlin Multiplatform
The annotations are a JVM library for now, so classes in `commonMain` can't use them yet. See
[proposal 0003](proposals/0003-kotlin-multiplatform.md) for the multiplatform support plan.

## Options
The processor accepts configuration options as KSP arguments, set in the module’s build file:

```kotlin
// build.gradle.kts
ksp {
    arg("jsonSchema.targets", "gemini,openai")
}
```

| Argument             | Value                                                          | Default       |
|----------------------|----------------------------------------------------------------|---------------|
| `jsonSchema.targets` | Sets the dialects to generate in the module, separated by commas    | Every dialect |

Setting only the dialects you use is a good idea, since invalid features on other unused dialects can still cause the
build to fail unnecessarily. For example, OpenAI doesn't accept maps, which are perfectly valid in the Gemini dialect.
See [Dialects](reference/targets.md#choosing-the-dialects) for the details, including how to set the dialects of a
single class from the code.

> [!TIP]
> Use generally supported features when targeting multiple providers; use different root classes when dialect-specific features are desired.

## A first schema
Annotate a class with `@SchemaRoot`:

```kotlin
package com.example

import com.vectornorthsystems.jsonschema.SchemaRoot

@SchemaRoot
data class Complex(
    val real: Double,
    val imaginary: Double,
)
```

Build the project. The processor generates an object in the same package, named after the class:

```kotlin
object ComplexSchema {
    const val GEMINI: String = """{"type":"object","properties":{"real":{"type":"number"},"imaginary":{"type":"number"}},"required":["real","imaginary"]}"""
    const val OPENAI: String = """{"type":"object","properties":{"real":{"type":"number"},"imaginary":{"type":"number"}},"additionalProperties":false,"required":["real","imaginary"]}"""

    fun getOrNull(target: SchemaTarget): String? = ...
    operator fun get(target: SchemaTarget): String = ...
}
```
The object will consist of:

* A string constant per dialect, holding the corresponding schema.
* Utilities like `getOrNull` and `operator get` to allow selecting the schema at runtime; see [Runtime targets](guides/runtime-targets.md).

It is also possible to choose the name of the generated object by using the `name` argument of the annotation, for example:
```kotlin`
@SchemaRoot(name = "ComplexJsonSchema")
```

Only the root needs `@SchemaRoot`. Its properties and corresponding types are described as part of the schema.
The source is written to `build/generated/ksp/` in case you want to read it.

## A first request
A schema is a string, so it goes wherever your HTTP client or SDK takes one. Most APIs want it as a JSON object
inside the request, so parse it rather than send it as a quoted string. With `kotlinx.serialization`:

```kotlin
val schema = Json.parseToJsonElement(ComplexSchema.GEMINI)
```

The schema placement varies by provider.

| Provider | API           | The schema goes in       | Guide                      |
|----------|---------------|--------------------------|----------------------------|
| Gemini   | Interactions  | `response_format.schema` | [Gemini](guides/gemini.md) |
| OpenAI   | Responses     | `text.format.schema`     | [OpenAI](guides/openai.md) |

The model answers with JSON that follows the schema, which decodes into the class it came from:

```kotlin
@Serializable
@SchemaRoot
data class Complex(val real: Double, val imaginary: Double)

val complex = Json.decodeFromString<Complex>(answer)
```

> [!NOTE]
> `@Serializable` is for `kotlinx.serialization`; this library doesn't depend on it and works with any decoder.

## Next
- [Concepts](concepts.md), for how defaults and nullability shape the schema in each dialect.
- [Types](reference/types.md) and [Annotations](reference/annotations.md), for everything a class can express.
- The [samples](../json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples), for runnable round trips.
