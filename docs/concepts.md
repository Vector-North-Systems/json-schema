# Concepts

## Code-first schemas
With structured output, the answer of an AI model is JSON that you decode into Kotlin classes. Those classes already
express what the answer looks like: its properties, their types, which ones can be `null`. A JSON Schema says the same
thing again, in another language.

This library derives the schema from the classes, so there's one description to maintain instead of two:

- The classes are what you edit. The schema is regenerated on every build.
- The schema and the decoder read the same classes, so they can't disagree.
- The schema is generated at compile time. Each one is a string constant, with no reflection and nothing to run when
  the app starts.
- What can't be described fails the build, pointing at the property, instead of failing in a request.

## Roots
A root is the class that describes a whole answer, marked with `@SchemaRoot`. The processor generates one object per
root, `{ClassName}Schema`, in the root's package.

The classes a root uses (i.e.: its properties), directly or through collections, are described inside its schema. They don't need an
annotation, and they don't get an object of their own unless they're also roots.

## Targets and dialects
Every AI provider takes a JSON Schema, but none takes all of JSON Schema. Each supports its own subset of keywords and
adds rules of its own. That subset and those rules are a **dialect**. For example:

- Gemini lets an object leave properties out of `required`. OpenAI's strict mode requires every property.
- OpenAI's strict mode needs `"additionalProperties": false` on every object. Gemini doesn't.
- Gemini accepts an object with arbitrary keys, which is how a `Map` is described. OpenAI's strict mode doesn't.

A **target** is a dialect the library generates, an entry of the `SchemaTarget` enum: `GEMINI`, `OPENAI`. Each root
gets one schema per target, and the generated object has one constant per target.

The schemas of a root come from the same description of the class. For each target, the library adapts it to the
dialect:

- What the dialect needs is added, like `additionalProperties` for OpenAI.
- What the dialect can't express is a build error, like a `Map` for OpenAI. The library doesn't drop a property or
  change its type to make a schema fit, because the answer would no longer decode into your class.

See [Dialects](reference/targets.md) for the full comparison, and for choosing which targets are generated.

## Required and nullable properties
For an AI model, required and nullable are different things:

- A **required** property must be in the answer.
- A **nullable** property may be `null` in the answer.

A Kotlin class expresses both. Nullability (`String?`) decides whether the schema allows `null`, in every dialect. A
default (`= ""`) says the class can be built without the value, so the model may leave the property out, in the
dialects that allow it.

| Kotlin                                   | Type in the schema   | `GEMINI` | `OPENAI` |
|------------------------------------------|----------------------|----------|----------|
| `val notes: String`                      | `"string"`           | Required | Required |
| `val notes: String?`                     | `["string", "null"]` | Required | Required |
| `val notes: String = ""`                 | `"string"`           | Optional | Required |
| `val notes: String? = null`              | `["string", "null"]` | Optional | Required |
| `@SchemaRequired val notes: String = ""` | `"string"`           | Required | Required |

- Primary constructor parameters without a default are required. Properties with a default, or declared in the class
  body, are optional, unless annotated with `@SchemaRequired`.
- Use `@SchemaRequired` when a default is only a convenience for your own code, and the model should still produce the
  value.
- Required and nullable (`String?` without a default) makes the model decide on `null` explicitly, rather than leave
  the property out.
- OpenAI's strict mode has no optional properties, so everything is required there and `@SchemaRequired` changes
  nothing. A property the model may not have a value for should be nullable: a `String = ""` property makes the model
  come up with a string.

### Classes that work in every dialect
To describe "the model may not know this" the same way for every provider, make the property nullable with a `null`
default:

```kotlin
val notes: String? = null
```

Gemini may leave it out or answer `null`, OpenAI answers `null`, and both decode to `null`.

### Decoding a missing property
When a property is optional, the model may leave it out of the response, and the decoder fills it in:

| Kotlin                      | kotlinx.serialization                 | Moshi, Jackson |
|-----------------------------|---------------------------------------|----------------|
| `val notes: String`         | Fails                                 | Fails          |
| `val notes: String?`        | Fails, unless `explicitNulls = false` | `null`         |
| `val notes: String = ""`    | `""`                                  | `""`           |
| `val notes: String? = null` | `null`                                | `null`         |

When the provider enforces the schema, a required property is always in the answer, so the first two rows don't come
up.

The `kotlinx.serialization` column has been verified. The Moshi and Jackson columns describe their documented
behavior, and will be checked by tests (see the [roadmap](roadmap.md)). Moshi and Jackson need their Kotlin support
(Moshi's Kotlin adapters, `jackson-module-kotlin`) to use defaults. Gson ignores Kotlin defaults and nullability, so a
missing property becomes `null`, even in a non-null property.

## Annotations
What the class can't say, annotations add: a description that tells the model what to put in a property, or a
constraint on its value, like a minimum or a date format. They're written as the matching JSON Schema keywords. See
[Annotations](reference/annotations.md).
