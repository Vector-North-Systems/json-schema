# OpenAI

Use the `OPENAI` constant of the generated object, e.g. `LibrarySchema.OPENAI`. It follows the
[schemas OpenAI supports](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses#supported-schemas)
for Structured Outputs in strict mode, where the answer is guaranteed to follow the schema.

## What the dialect generates
Strict mode adds rules that the library applies for you:

- **Every property is required**, whether or not it has a default.
- **Every object has `"additionalProperties": false`**, including nested objects and the ones in `$defs`.
- **Maps are a build error**, since a map is an object with arbitrary keys. See
  [Replacing a map](#replacing-a-map).
- **A reference with a title or a description is wrapped in `anyOf`**, since strict mode rejects other keywords next to
  a `$ref`: `{"description": "...", "anyOf": [{"$ref": "#/$defs/Address"}]}`. It applies to classes written in
  `$defs`, the ones that are recursive or used more than once.

The rest is the same as in the other dialects: nullable properties allow `null` (`"type": ["string", "null"]`), and
titles, descriptions, formats and the minimum and maximum constraints are written as their keywords.

For example, this class:

```kotlin
@SchemaRoot
data class Recipe(
    val instructions: String,
    val ingredients: List<Ingredient>,
    val isColdDish: Boolean? = null
)
```

has `isColdDish` in `required`, although it has a default:

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
        "additionalProperties": false,
        "required": ["name"]
      }
    },
    "isColdDish": {"type": ["boolean", "null"]}
  },
  "additionalProperties": false,
  "required": ["instructions", "ingredients", "isColdDish"]
}
```

See [Dialects](../reference/targets.md#how-the-dialects-differ) for the comparison with the others.

## Sending the schema
With the [Responses API](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses), the
schema goes in `text.format`, with a name and `strict` set to `true`. This example uses Ktor and
`kotlinx.serialization`:

```kotlin
val request = buildJsonObject {
    put("model", "<model>")
    put("input", "Classify the following books: ...")
    putJsonObject("text") {
        putJsonObject("format") {
            put("type", "json_schema")
            put("name", "library")
            put("strict", true)
            // Parsed, so the schema is sent as a JSON object rather than a quoted string.
            put("schema", Json.parseToJsonElement(LibrarySchema.OPENAI))
        }
    }
}

val response = client.post("https://api.openai.com/v1/responses") {
    header("Authorization", "Bearer ${System.getenv("OPENAI_API_KEY")}")
    contentType(ContentType.Application.Json)
    setBody(request.toString())
}
```

It's adapted from the samples'
[`OpenAi.kt`](../../json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/providers/OpenAi.kt).

### With Chat Completions
The schema goes in `response_format`, inside `json_schema`:

```kotlin
putJsonObject("response_format") {
    put("type", "json_schema")
    putJsonObject("json_schema") {
        put("name", "library")
        put("strict", true)
        put("schema", Json.parseToJsonElement(LibrarySchema.OPENAI))
    }
}
```

## Reading the answer
The answer is in the `output` of the response, in the `output_text` content of its `message` items. Check two things
before decoding, as
[`OpenAi.kt`](../../json-schema-tests/src/main/kotlin/com/vectornorthsystems/samples/providers/OpenAi.kt) does:

- **The `status` is `completed`.** An `incomplete` response was cut short, e.g. by `max_output_tokens`, so its JSON is
  truncated and won't decode.
- **There's no `refusal` content.** When the model refuses, the refusal replaces the answer, and there's no JSON.

Then it decodes straight into the root class:

```kotlin
val library = Json.decodeFromString<Library>(outputText)
```

## Things to watch out for
- **There are no optional properties.** A property with a default is still required, so the model always produces a
  value for it. If the model may not have one, make the property nullable (`String?`) and it answers `null`. A
  non-null property with a default, like `val notes: String = ""`, makes the model come up with a string.
  `@SchemaRequired` changes nothing in this dialect. See
  [Required and nullable properties](../concepts.md#required-and-nullable-properties).
- **Maps fail the build**, for every root that generates the `OPENAI` schema, which is all of them by default. If you
  don't call OpenAI, or a class is only sent to another provider, leave the dialect out with
  [`jsonSchema.targets` or `@SchemaRoot(targets)`](../reference/targets.md#choosing-the-dialects).
- **OpenAI's limits aren't checked.** OpenAI limits the size of a schema: the number of properties, how deep objects
  nest, the number of enum values. The library allows objects to nest 32 deep, which is more than OpenAI accepts. A
  schema over a limit is generated, and rejected by the API. See OpenAI's
  [supported schemas](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses#supported-schemas)
  for the current limits.
- **Use strict mode.** The schema is written for `"strict": true`. Without it, OpenAI doesn't guarantee the answer
  follows the schema.

### Replacing a map
If the keys are known, use a class, with a property per key:

```kotlin
// Instead of val scores: Map<String, Int>
data class Scores(val home: Int, val away: Int)
```

If they aren't, use a list of entries:

```kotlin
data class Score(val player: String, val points: Int)

@SchemaRoot
data class Match(val scores: List<Score>)
```

and turn it into a map after decoding: `match.scores.associate { it.player to it.points }`.
