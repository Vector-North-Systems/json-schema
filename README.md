# Kotlin JSON Schema Generator
[![Maven Central](https://img.shields.io/maven-central/v/com.vectornorthsystems/json-schema-annotations)](https://central.sonatype.com/artifact/com.vectornorthsystems/json-schema-annotations)
[![CI](https://github.com/Vector-North-Systems/json-schema/actions/workflows/ci.yml/badge.svg)](https://github.com/Vector-North-Systems/json-schema/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE.txt)

Generates a [JSON Schema](https://json-schema.org/overview/what-is-jsonschema) from Kotlin classes at compile time, using
[KSP](https://kotlinlang.org/docs/ksp-overview.html).

JSON Schema is a standard for describing the structure and rules of JSON data. One common use is structured output from
generative AI models: you send a schema with the request, and the model produces JSON that adheres to it.
The schemas produced by this library follow the subset of JSON Schema
that [Gemini supports](https://ai.google.dev/gemini-api/docs/structured-output#json-schema-support). Support for the
subsets of other major AI providers is planned; see [Roadmap](#roadmap).

Your Kotlin classes are the single source of truth. Change them as your needs change (add a property, nest a
new class, make a field optional), and the next build regenerates a matching schema. Furthermore, the model's response
maps straight back into the same classes, so there's no hand-written schema to keep in sync with your code.

Because the schemas are generated at compile time, there's no reflection and no runtime dependency: each schema is a
string constant in a generated object. Mistakes, like a constraint on the wrong type or a type that can't be described,
fail the build and point at the property.

## Installation
Apply the [KSP plugin](https://github.com/google/ksp/releases) that matches your Kotlin version, then add the annotations
and the processor:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("com.vectornorthsystems:json-schema-annotations:0.3.0")
    ksp("com.vectornorthsystems:json-schema-ksp:0.3.0")
}
```

Both artifacts are on Maven Central. Requires JDK 17 or later. Built and tested with Kotlin 2.4.20 and KSP 2.3.10.

## Usage
For example, consider the following class:
```kotlin
@SchemaRoot
data class Complex(
    val real: Double,
    val imaginary: Double,
)
```

`@SchemaRoot` marks the class for processing, which generates the following object in the same package:

```kotlin
object ComplexSchema {
    const val GEMINI: String = """{"type":"object","properties":{"real":{"type":"number"},"imaginary":{"type":"number"}},"required":["real","imaginary"]}"""
    // ...Other fields
}
```

The object exposes a constant property per supported dialect.
Use the constant to access the schema you need, `ComplexSchema.GEMINI` in the example. See [Dialects](#dialects) for more.

The schemas are minified. The examples below are formatted for readability, so the one above reads:

```json
{
  "type": "object",
  "properties": {
    "real": {"type": "number"},
    "imaginary": {"type": "number"}
  },
  "required": ["real", "imaginary"]
}
```

To choose the generated object's name, use `@SchemaRoot(name = "ComplexJsonSchema")`.
The object is public, or `internal` when the class is internal or private.

The schema follows the structure and types of the class, including nullability.
See [Required and nullable properties](#required-and-nullable-properties).

Another example:

```kotlin
data class Ingredient(
    val name: String
)

@SchemaRoot
data class Recipe(
    val instructions: String,
    val ingredients: List<Ingredient>,
    val isColdDish: Boolean? = null
)
```
Produces the schema below for the `GEMINI` dialect. In this case `isColdDish` allows `null`, and the AI model may
leave it out, since it has a default. In this dialect, constructor parameters without a default are marked required,
and properties with a default are optional.
```json
// GEMINI dialect
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

Most primitive and common collection types are supported; see [Types](#types) for how each one is mapped.

Annotations add descriptions and constraints, see [Annotations](#annotations) for the full list:

```kotlin
@SchemaRoot
data class Event(
    @SchemaTitle("Name")
    @SchemaDescription("The name of the event, as advertised")
    val name: String,

    val notes: String? = null,

    @SchemaFormat(SchemaFormat.Format.DATE_TIME)
    val startsAt: String,

    @SchemaMinimum(1.0)
    @SchemaMaximum(4.5)
    val rating: Double,

    @SchemaMinimum(0.0)
    val attendees: Int = 0,

    @SchemaMinItems(1)
    @SchemaMaxItems(20)
    val speakers: List<String>
)
```
Produces:
```json
{
  "type": "object",
  "properties": {
    "name": {
      "title": "Name",
      "description": "The name of the event, as advertised",
      "type": "string"
    },
    "notes": {"type": ["string", "null"]},
    "startsAt": {"type": "string", "format": "date-time"},
    "rating": {"type": "number", "minimum": 1, "maximum": 4.5},
    "attendees": {"type": "integer", "minimum": 0},
    "speakers": {
      "type": "array",
      "items": {"type": "string"},
      "minItems": 1,
      "maxItems": 20
    }
  },
  "required": ["name", "startsAt", "rating", "speakers"]
}
```
These examples are checked by `ReadmeTest` in `json-schema-tests`; check the code for more details.

### Required and nullable properties
For the AI model, nullable and optional mean different things. A nullable field has `null` among its types, so the
model can answer an explicit JSON `null`. An optional field is left out of `required`, so the model can leave it out
of the response entirely. The Kotlin class already expresses both: nullability (`String?`) decides whether the schema
allows `null`, and a default (`= ""`) decides whether the field is required:

| Kotlin                                    | Schema                                   | Field missing: kotlinx.serialization        | Field missing: Moshi, Jackson |
|-------------------------------------------|------------------------------------------|---------------------------------------------|-------------------------------|
| `val notes: String`                       | Required, `"type": "string"`             | Fails                                       | Fails                         |
| `val notes: String?`                      | Required, `"type": ["string", "null"]`   | Fails, unless `explicitNulls = false`       | `null`                        |
| `val notes: String = ""`                  | Optional, `"type": "string"`             | `""`                                        | `""`                          |
| `val notes: String? = null`               | Optional, `"type": ["string", "null"]`   | `null`                                      | `null`                        |
| `@SchemaRequired val notes: String = ""`  | Required, `"type": "string"`             | `""`                                        | `""`                          |

When the AI provider enforces the schema, a required field is always in the response, so a decoder never has to fill
in a missing one. Required and nullable (`String?` without a default) makes the AI model decide on `null` explicitly,
rather than leave the field out.

The `kotlinx.serialization` column has been verified. The Moshi and Jackson columns describe their documented
behavior, and will be checked by tests (see [Roadmap](#roadmap)).

Use `@SchemaRequired` for a property whose default is only a convenience for your own code, when the AI model should
still produce it. Moshi and Jackson need their Kotlin support (Moshi's Kotlin adapters, `jackson-module-kotlin`) to
use defaults. Gson ignores Kotlin defaults and nullability, so a missing field becomes `null`, even in a non-null
property.

## Sending the schema to Gemini
`LibrarySchema.GEMINI` holds the schema as a JSON string, ready to put in a request. With Gemini's
[Interactions API](https://ai.google.dev/gemini-api/docs/interactions/structured-output), it goes in
`response_format.schema`. This example, adapted from the samples'
[`Library.kt`](json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/Library.kt) and [`Gemini.kt`](json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/Gemini.kt), uses `Ktor` and `kotlinx.serialization`:

```kotlin
@Serializable
data class Book(
    @SchemaDescription("The title of the book")
    val title: String,

    @SchemaMaxItems(3)
    val authors: List<String> = emptyList(),

    @SchemaMinimum(1.0)
    @SchemaMaximum(5.0)
    val rating: Double? = null,
)

@Serializable
@SchemaRoot
data class Library(
    val books: List<Book>,
)

val request = buildJsonObject {
    put("model", "gemini-3.8-flash")
    put("input", "Classify the following books: ...")
    putJsonObject("response_format") {
        put("type", "text")
        put("mime_type", "application/json")
        // Parsed, so the schema is sent as a JSON object rather than a quoted string.
        put("schema", Json.parseToJsonElement(LibrarySchema.GEMINI))
    }
}

val response = client.post("https://generativelanguage.googleapis.com/v1beta/interactions") {
    header("x-goog-api-key", System.getenv("GEMINI_API_KEY"))
    header("Api-Revision", "2026-05-20")
    contentType(ContentType.Application.Json)
    setBody(request.toString())
}
```

The model's answer is JSON that matches the schema, in the text of the response's `model_output` steps
([`Gemini.kt`](json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/Gemini.kt) shows how to read it). It decodes straight into the root class:

```kotlin
val library = Json.decodeFromString<Library>(outputText)
```

Change `Book` or `Library`, and the schema, the request and the decoding all follow on the next build.

With the `generateContent` API, pass the schema as `generationConfig.responseJsonSchema`, with `responseMimeType` set
to `application/json`. Don't use `responseSchema`: it takes Gemini's OpenAPI-based `Schema` object, a different format.

## Reference

Annotate a class with `@SchemaRoot` and use the generated `${ClassName}Schema` object, in the same package, or the
object named by `@SchemaRoot(name = "...")`. Nested classes with the same name in the same package need different
names. The object has a `String` constant per dialect, named after it (`GEMINI`), and `get(target)` and
`getOrNull(target)` to pick one at runtime. The rules below describe the schemas it holds.

### Dialects
Each AI provider accepts its own subset of JSON Schema, so the schemas are generated per dialect, listed in
`SchemaTarget`. Today that's `GEMINI` only (see [Roadmap](#roadmap)).

By default, every dialect is generated. To narrow them down for a module, pass a comma separated list, as a
KSP argument:

```kotlin
// build.gradle.kts
ksp {
    arg("jsonSchema.targets", "gemini")
}
```

A class can set its own dialects, overriding the module's:

```kotlin
@SchemaRoot(targets = [SchemaTarget.GEMINI])
data class Complex(val real: Double, val imaginary: Double)
```

For a dialect that wasn't generated for the class, `getOrNull(target)` gives `null`, and `get(target)` throws an
`IllegalArgumentException` that says where to add it.

### Types
| Kotlin                                                            | Schema                                                 |
|-------------------------------------------------------------------|--------------------------------------------------------|
| `Int`, `Long`, `Short`, `Byte`                                    | `integer`                                              |
| `UInt`, `ULong`, `UShort`, `UByte`                                | `integer` with `minimum` 0                             |
| `Float`, `Double`                                                 | `number`                                               |
| `Boolean`                                                         | `boolean`                                              |
| `String`, `Char`                                                  | `string`                                               |
| Enum classes                                                      | `string` with `enum` set to the entry names            |
| `Array`, `List`, `Set`, `Collection`, `Iterable` (mutable or not) | `array`, with `items` from the element type      |
| `IntArray`, `BooleanArray` and the other primitive arrays         | `array` of the matching primitive                      |
| `Map<String, V>` and `MutableMap<String, V>`                      | `object` with `additionalProperties` from `V`          |
| Other classes                                                     | `object` with their `properties`                       |

### Properties
- Only properties with a backing field are included: computed and delegated properties aren't data, so they're left out.
  Inherited properties are included.
- Property names are the Kotlin names. See [Roadmap](#roadmap) for plans to extend this.
- Primary constructor parameters without a default are required, matched to properties by name in the class being
  described, so an inherited property is required when the subclass's constructor takes it without a default. See
  [Known Issues](#known-issues) for classes from compiled libraries.
- Properties with a default or declared in the class body are optional (they always have an initial value), unless
  annotated with `@SchemaRequired`.
- Nullable types allow `null` (`"type": ["string", "null"]`), which is independent of a property being required.
  A nullable enum also lists `null` among its values (`"enum": ["METRIC", "IMPERIAL", null]`), since `enum` restricts
  the value on top of `type`.
- Type aliases are resolved to the type they stand for, keeping their nullability.
- Value classes (`@JvmInline value class UserId(val value: String)`) are described by the type they wrap, as
  kotlinx.serialization encodes them as the bare value.

### Generics and recursion
- Type parameters take the type arguments of each use, so `Box<String>` and `Box<Int>` get different schemas.
  `@SchemaRoot` classes can't have type parameters.
- A root class that contains itself is referenced with `"$ref": "#"`. A nested class that contains itself, or is
  used in more than one place, is defined once in `$defs` and referenced with `"$ref": "#/$defs/<Name>"`. A nullable
  reference becomes `"anyOf": [{"$ref": ...}, {"type": "null"}]`.
- Objects can nest at most 32 deep, which catches generic types that grow without repeating, e.g. a `Box<T>` holding a
  `Box<List<T>>`.

### Annotations
| Annotation                                   | Applies to                    | Keyword/Notes               |
|----------------------------------------------|-------------------------------|-----------------------------|
| `@SchemaRoot`                                | A class without type parameters | Entry point of the schema |
| `@SchemaTitle`, `@SchemaDescription`         | Any property                  | `title`, `description`      |
| `@SchemaRequired`                            | Any property                  | Added to `required`, even with a default |
| `@SchemaFormat`                              | `String` (not enums)          | `format`: `date-time`, `date` or `time` |
| `@SchemaMinimum`, `@SchemaMaximum`           | Integers and numbers          | `minimum`, `maximum` (override the 0 of unsigned types) |
| `@SchemaMinItems`, `@SchemaMaxItems`         | Collections                   | `minItems`, `maxItems`      |
| `@SchemaExclude`                             | Any property                  | Excludes the property from the resulting schema |


## Not supported
Gemini's `prefixItems` support was deprecated, and it is now removed. AI models follow `prefixItems` and produce arrays of
mixed types, which a Kotlin collection can't hold, so the response fails to decode. Use a data class instead: its
properties decode, and their names tell the model what each value means.

## Build errors
These fail the build, pointing at the property:
- An annotation on a property it doesn't apply to, a minimum greater than its maximum, or a negative
  `@SchemaMinItems` or `@SchemaMaxItems`.
- NaN or either infinity in `@SchemaMinimum` or `@SchemaMaximum`; bounds must be finite.
- `Any`, an unresolved type parameter, or a map key that isn't `String`.
- Interfaces, sealed classes and abstract classes, until `anyOf` is supported.

These fail the build, pointing at the class:
- A root with type parameters, or an enum, interface, annotation class, sealed class or abstract class used as a root.
  Roots must be concrete classes or objects; enum properties remain supported.
- A `name` that isn't a valid Kotlin identifier, or an object name already generated in the package.
- A dialect in `@SchemaRoot(targets)` that the processor doesn't know.

These fail the build too, without pointing at code:
- An unknown dialect in the `jsonSchema.targets` KSP argument, or an empty list.

Unknown `jsonSchema.*` KSP arguments are ignored with a warning, since they are likely typos.

Java classes aren't mapped yet, so they're described by their private fields or fail (see [Roadmap](#roadmap)).

## Known Issues
- **Generated filename collisions.** Custom object names such as `foo` and `Foo` both produce `Foo.kt`, causing
  a build failure. Choose names that differ beyond capitalization of the first character.
  See [#8](https://github.com/Vector-North-Systems/json-schema/issues/8).
- **Classes from compiled libraries.** Required properties come from constructor defaults, which haven't been tested
  for classes from another module or a library jar. If KSP can't read their defaults, every constructor parameter is
  required. Decoding still works, but the AI model can't leave out fields that have a default. Until this is
  verified, check the generated schema, or describe these classes with your own types.
  See [#9](https://github.com/Vector-North-Systems/json-schema/issues/9).
- **See [all open bugs](https://github.com/Vector-North-Systems/json-schema/issues?q=is%3Aissue+state%3Aopen+type%3ABug)**.

## Roadmap
Planned, roughly in order. Features with design decisions to make have a proposal in
[`docs/proposals`](docs/proposals), linked below.

1. **Serialization names.** Use the names the decoder expects and leave out `@Transient` properties, starting with
   kotlinx.serialization's `@SerialName`, then Moshi's `@Json(name)`, Gson's `@SerializedName` and Jackson's
   `@JsonProperty`. Decoding tests will verify the missing-field behavior in
   [Required and nullable properties](#required-and-nullable-properties) for each supported decoder.
2. **Sealed classes through `anyOf`.** Each subclass becomes an option, with a discriminator property so the response
   can be decoded. Later, `@SchemaSubtypes` for interfaces and abstract classes, whose subclasses the compiler doesn't
   know.
3. **Java types.** Boxed primitives, `java.util` collections and maps, `java.time` types (as strings with a `format`),
   `UUID`, `BigDecimal` and `BigInteger`.
4. **OpenAI and Anthropic schemas.** Both use JSON Schema for structured output, but each accepts a different subset
   than Gemini.
5. **Property order.** Gemini 2.5 and later keep the schema's key order in the output, so declaration order shapes
   generation (e.g. reasoning before answer). The library’s ordering guarantee will be documented and tested, including
   inherited properties.
6. **Enums for numbers and strings.** `@SchemaEnum` restricts an `Int`, `Double` or `String` property to a set of
   values, without declaring an enum class. See [proposal 0001](docs/proposals/0001-schema-enum.md).
7. **Titles and descriptions on classes.** Describe a type once, on the class, instead of on every property that uses
   it. Also describes the root schema and enum classes. See
   [proposal 0002](docs/proposals/0002-class-descriptions.md).
8. **Kotlin Multiplatform.** Multiplatform annotations, so schema classes can live in `commonMain`. See
   [proposal 0003](docs/proposals/0003-kotlin-multiplatform.md).
9. **Readable `$defs` names.** For generics: names like `Box_String` instead of `Box2`; making reading, understanding and
   debugging the schema an easier task. See [#11](https://github.com/Vector-North-Systems/json-schema/issues/11)
