# Gemini

Use the `GEMINI` constant of the generated object, e.g. `LibrarySchema.GEMINI`. It follows the
[subset of JSON Schema that Gemini supports](https://ai.google.dev/gemini-api/docs/structured-output#json-schema-support).

## What the dialect generates
- Properties without a default are required. Properties with a default are optional, so the model may leave them out,
  unless they have `@SchemaRequired`.
- Nullable properties allow `null`: `"type": ["string", "null"]`.
- A `Map<String, V>` is an object whose `additionalProperties` describe `V`.
- Titles, descriptions, formats and the minimum and maximum constraints are written as their keywords.

See [Dialects](../reference/targets.md#how-the-dialects-differ) for the comparison with the others.

## Sending the schema
With the [Interactions API](https://ai.google.dev/gemini-api/docs/interactions/structured-output), the schema goes in
`response_format.schema`. This example uses Ktor and `kotlinx.serialization`:

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

// @SchemaRoot generates LibrarySchema; @Serializable lets kotlinx.serialization decode the response.
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

It's adapted from the samples'
[`Library.kt`](../../json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/Library.kt) and
[`Gemini.kt`](../../json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/providers/Gemini.kt).

### With `generateContent`
Pass the schema as `generationConfig.responseJsonSchema`, with `responseMimeType` set to `application/json`.

## Reading the answer
The model's answer is JSON that follows the schema, in the text of the response's `model_output` steps
([`Gemini.kt`](../../json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/providers/Gemini.kt) shows how
to read it). It decodes straight into the root class:

```kotlin
val library = Json.decodeFromString<Library>(outputText)
```

Change `Book` or `Library`, and the schema, the request and the decoding all follow on the next build.

## Recursive properties
Gemini rejects the schema of a class with a required, nullable property of its own type, with an error that doesn't
say why: `Invalid JSON payload: syntax error in request body`. So it's a build error in this dialect:

```kotlin
data class Category(
    val name: String,
    val subcategories: List<Category>,  // Fine: a list can be empty.
    val parent: Category?,              // Build error: required, and leads back to Category.
)
```

Give the property a default, which makes it optional:

```kotlin
    val parent: Category? = null,
```

The model may then leave it out or answer `null`, and both decode to `null`. The same applies when the property leads
back to its class through other classes, if every property on the way is required. A list or a map on the way, or an
optional property, is accepted.

## Things to watch out for
- **`responseSchema` is a different format.** With `generateContent`, don't pass the schema as `responseSchema`: it
  takes Gemini's OpenAPI-based `Schema` object. Use `responseJsonSchema`.
- **Optional properties may be missing.** A property with a default can be left out of the answer, and the decoder
  uses the default. If the model should always produce it, add `@SchemaRequired`. See
  [Required and nullable properties](../concepts.md#required-and-nullable-properties).
- **`prefixItems` isn't supported.** Models follow `prefixItems` and produce arrays of mixed types, which a Kotlin
  collection can't hold, so the answer fails to decode. The `@SchemaPrefixItems` annotation was removed in 0.3.0. Use
  a data class instead: its properties decode, and their names tell the model what each value means.
- **Property order.** Gemini 2.5 and later keep the schema's key order in the answer, so the order of the properties
  can shape generation, e.g. reasoning before the answer. Properties are written in declaration order today, but the
  guarantee isn't documented and tested yet; see the [roadmap](../roadmap.md).
