# Dialects

Each AI provider accepts its own subset of JSON Schema, with its own rules: a dialect. The dialects the library
generates are the entries of the `SchemaTarget` enum, and every `@SchemaRoot` class gets one schema per dialect. See
[Concepts](../concepts.md#targets-and-dialects) for the idea.

## Targets
| `SchemaTarget` | `jsonSchema.targets` value | Provider      | Follows                                                                                                                                         | Guide                         |
|----------------|----------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| `GEMINI`       | `gemini`                   | Google Gemini | [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output#json-schema-support)                                         | [Gemini](../guides/gemini.md) |
| `OPENAI`       | `openai`                   | OpenAI        | [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses#supported-schemas), strict mode | [OpenAI](../guides/openai.md) |

More are planned; see the [roadmap](../roadmap.md).

## The generated object
For a root named `Recipe`, the object is `RecipeSchema`, in the same package:

| Member                                    | What it gives                                                                                |
|-------------------------------------------|----------------------------------------------------------------------------------------------|
| `const val GEMINI: String`, `OPENAI`, ... | The schema in that dialect, as minified JSON. There's one per dialect generated for the root. |
| `getOrNull(target: SchemaTarget): String?` | The schema in `target`, or `null` if it isn't generated for the root.                        |
| `get(target: SchemaTarget): String`       | The schema in `target`. Throws an `IllegalArgumentException` if it isn't generated.          |

See [Runtime targets](../guides/runtime-targets.md) for `get` and `getOrNull`.

## Choosing the dialects
By default, every dialect is generated for every root.

### For a module
Pass the dialects as a KSP argument, a comma separated list, in any case:

```kotlin
// build.gradle.kts
ksp {
    arg("jsonSchema.targets", "gemini")
}
```

An unknown dialect or an empty list fails the build.

### For a class
A class can set its own dialects, which replace the module's:

```kotlin
@SchemaRoot(targets = [SchemaTarget.GEMINI])
data class Scores(val byPlayer: Map<String, Int>)
```

| `jsonSchema.targets` | `@SchemaRoot(targets)` | Generated for the class |
|----------------------|------------------------|-------------------------|
| Not set              | Not set                | Every dialect           |
| `gemini`             | Not set                | `GEMINI`                |
| Not set              | `[OPENAI]`             | `OPENAI`                |
| `gemini`             | `[OPENAI]`             | `OPENAI`                |

### Why narrow them down
A class is checked against the rules of every dialect it's generated for. If a dialect can't express something in the
class, the build fails, even if you never use that dialect's schema. Generate only the dialects you send.

## How the dialects differ
Everything that isn't in this table is the same in every dialect, and is described in [Types](types.md) and
[Annotations](annotations.md).

|                                                  | `GEMINI`                                                       | `OPENAI`             |
|--------------------------------------------------|----------------------------------------------------------------|----------------------|
| Required properties                              | Constructor parameters without a default, and `@SchemaRequired` | Every property       |
| Properties with a default                        | Optional                                                       | Required             |
| `additionalProperties` on objects                | Not written                                                    | `false`              |
| `Map<String, V>`                                 | An object with `additionalProperties` from `V`                 | Build error          |
| Nullable types                                   | `"type": [..., "null"]`                                        | The same             |
| Titles, descriptions, formats, minimum, maximum  | Written                                                        | Written              |
| Recursion and shared classes (`$ref`, `$defs`)   | Supported                                                      | Supported            |
| A required, nullable property that leads back to its own class | Build error                                      | Supported            |
| A `$ref` with a title or a description           | Next to the `$ref`                                             | Around it: `"anyOf": [{"$ref": ...}]` |

For the same class:

```kotlin
data class Ingredient(val name: String)

@SchemaRoot
data class Recipe(
    val instructions: String,
    val ingredients: List<Ingredient>,
    val isColdDish: Boolean? = null
)
```

`RecipeSchema.GEMINI` is:

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

### `OPENAI`, compared with `GEMINI`
```diff
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
