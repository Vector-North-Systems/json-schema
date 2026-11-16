# Why this library

Structured outputs let you hand an AI model a JSON Schema and get back JSON that matches it. In Kotlin, that schema
usually ends up hand-written, kept in sync with your classes by discipline alone, and adjusted for each provider's
subset of JSON Schema. This library removes all three problems: your Kotlin classes are the schema, it's generated at
compile time for each provider you target, and the response decodes straight back into the same classes.

## The problem

Every major provider now accepts JSON Schema for structured outputs, but each one supports a different part of it.
OpenAI's strict mode requires every property to be listed in `required` and every object to be closed with
`"additionalProperties": false`. Gemini accepts optional properties and maps. Anthropic doesn't enforce numeric bounds.
A schema that works with one provider can be rejected by another, or accepted and quietly interpreted differently.

Without a generator, a Kotlin developer has to:

- **Write the schema by hand,** as a JSON string or with an SDK's builder objects, next to the Kotlin class it
  describes. Every change to the class needs the same change in the schema, and nothing checks that they agree. The
  mismatch shows up at runtime, as a response that doesn't decode.
- **Get the subtle parts right.** A nullable property and an optional property are different things in JSON Schema. A
  nullable enum needs `null` among its `enum` values, not just in its `type`, or validators reject a correct `null`.
  These are easy to miss, and the model's output won't always reveal the mistake.
- **Maintain one schema per provider,** when the same data comes from more than one.

## Your classes are the schema

Annotate a class, and the build generates its schema:

```kotlin
@Serializable
@SchemaRoot
data class Recipe(
    @SchemaDescription("The name of the recipe")
    val name: String,

    @SchemaDescription("Preparation time in minutes")
    @SchemaMinimum(1.0)
    val prepTimeMinutes: Int? = null,

    val difficulty: Difficulty? = null,

    @SchemaMinItems(1)
    val ingredients: List<Ingredient>,
)

enum class Difficulty { EASY, MEDIUM, HARD }

@Serializable
data class Ingredient(val name: String, val quantity: String)
```

Without the library, this is the Gemini schema you'd write and maintain by hand:

```json
{
  "type": "object",
  "properties": {
    "name": {"description": "The name of the recipe", "type": "string"},
    "prepTimeMinutes": {"description": "Preparation time in minutes", "type": ["integer", "null"], "minimum": 1},
    "difficulty": {"type": ["string", "null"], "enum": ["EASY", "MEDIUM", "HARD", null]},
    "ingredients": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {"type": "string"},
          "quantity": {"type": "string"}
        },
        "required": ["name", "quantity"]
      },
      "minItems": 1
    }
  },
  "required": ["name", "ingredients"]
}
```

With it, the schema is a constant, and the response decodes into the class that produced it:

```kotlin
val schema = RecipeSchema.GEMINI

// Send the schema with the HTTP client or SDK you already use, then:
val recipe = Json.decodeFromString<Recipe>(responseText)
```

The Kotlin class already says everything the schema needs. A property without a default is required; a nullable type
allows `null`; a default makes the property optional; an enum class lists its values. The library reads those
decisions instead of asking you to restate them, so the schema and the decoder can't disagree. Change the class, and
the next build changes the schema with it.

## One class, every dialect

Each provider's subset of JSON Schema is a dialect, and the library generates one schema per dialect from the same
class. Choose the dialects for a module once:

```kotlin
// build.gradle.kts
ksp { arg("jsonSchema.targets", "gemini,openai") }
```

For OpenAI, the same `Recipe` produces a schema with every property required and every object closed:

```diff
       "items": {
         "type": "object",
         "properties": {...},
-        "required": ["name", "quantity"]
+        "required": ["name", "quantity"],
+        "additionalProperties": false
       },
       "minItems": 1
     }
   },
-  "required": ["name", "ingredients"]
+  "required": ["name", "prepTimeMinutes", "difficulty", "ingredients"],
+  "additionalProperties": false
 }
```

Your code picks the schema for the provider it calls, `RecipeSchema.OPENAI` here, or `RecipeSchema[target]` when the
provider is chosen at runtime. The responses decode into the same `Recipe` either way. Under OpenAI, the model always
writes every field, writing `null` where it has no value, so defaults never fill in; the decoded object is otherwise
the same.

Providers change what they support over time, and new ones arrive. When that happens, the change belongs in the
library's dialect, not in every schema you've written.

## Mistakes fail the build

Because the schemas are generated at compile time, problems surface where they're cheapest to fix: in the build,
pointing at the property that caused them.

- A constraint on the wrong type, such as `@SchemaMinimum` on a `String`, is a build error.
- A type that can't be described, or can't be described in a dialect you target, is a build error that says why and
  what to use instead. A `Map` can't be expressed in OpenAI's strict mode, for example, so targeting OpenAI with a map
  property fails with a suggestion: a data class for known keys, or a list of key-value entries.
- A feature that a dialect can't enforce, but that's safe to relax, is a warning, never a silent change.

There's also nothing to pay at runtime: no reflection, no schema building on startup, and no dependency beyond the
annotations. Each schema is a string constant in your compiled code.

## How it compares

**Hand-written schemas** work for one small class and one provider. They stop scaling with the second class that
nests the first, the second provider, or the first refactoring. It is very hard (and tedious) to maintain the schema and the code at 
the same time.

**SDK schema builders** replace JSON text with objects, but you still describe every property twice: once in the class,
once in the builder. They're also tied to one provider's SDK, so working with multiple providers takes more effort.

**Schema-first code generation** turns the problem around: a JSON Schema file is the source of truth, and a generator produces the 
classes. It's the right model when the schema is a contract shared across languages or owned by another team. For structured outputs, 
however, the dialects work against it. Multiple AI providers force you to maintain one schema per provider, generating near-identical 
classes from each, or settle for the subset every provider accepts. The schema is still hand-written JSON, the generated classes 
are rewritten on every run, so customizing them is limited, and how a generator maps nullable and optional properties, and which 
serialization library it targets, varies by tool.

**OpenAI's Java SDK** can derive a schema from a class at runtime, using reflection and Jackson annotations. If you only
call OpenAI, from the JVM, with Jackson, it's a good option. This library differs in what happens around that: schemas
for several providers from one class, generated at compile time, matched to kotlinx.serialization's decoding, and
usable with any HTTP client or SDK.

**Other languages** have had this for a while. The providers' documentation shows Python examples with Pydantic and
TypeScript examples with Zod, where a class or a type is the schema. This library brings the same approach to Kotlin,
with Kotlin's own definitions of required, nullable and optional.

## When it's not the right fit

- **A schema that's a shared contract,** used by several languages or owned by another team, is better kept as a JSON
  file, with classes generated from it. This library suits the opposite case: your Kotlin code owns the data model.
- **Schemas built at runtime,** for example from user input or a database, can't be generated at compile time. Build
  those with a JSON library.
- **Java classes** aren't processed. The processor reads Kotlin declarations; Java types used inside Kotlin classes are
  [on the roadmap](roadmap.md).
- **Shapes a dialect can't express** stay out of reach for that dialect. The build tells you, and the fix is a class
  designed for that provider, not a schema that only some providers accept.

## Further reading

- [JSON Schema](https://json-schema.org): the specification, and the
  [Understanding JSON Schema](https://json-schema.org/understanding-json-schema) guide.
- [Efficient Guided Generation for Large Language Models](https://arxiv.org/abs/2307.09702) (Willard and Louf, 2023):
  how constrained decoding makes a model's output follow a schema.
- [JSONSchemaBench](https://arxiv.org/abs/2501.10868) (2025): a benchmark of about 10,000 real-world schemas across
  structured output frameworks and APIs, measuring how much of JSON Schema each one supports.
- [The only schema language AI speaks is JSON Schema](https://www.sourcemeta.com/blog/ai-only-speaks-json-schema/)
  (Sourcemeta): why providers converged on JSON Schema, and how their support still diverges.
- [Structured output](https://aiwiki.ai/wiki/structured_output) (AI Wiki): a broad overview of the field, its history
  and its tools.