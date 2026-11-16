# Runtime targets

When the provider is known at compile time, use its constant: `RecipeSchema.GEMINI`. When it's chosen at runtime, by
configuration or to fall back from one provider to another, pick the schema with a `SchemaTarget`.

## `get` and `getOrNull`
Every generated object has both:

```kotlin
val schema: String = RecipeSchema[target]            // Throws if the dialect isn't generated for Recipe.
val schema: String? = RecipeSchema.getOrNull(target) // Returns null instead.
```

`RecipeSchema[target]` is the operator form of `RecipeSchema.get(target)`.

## Choosing a provider from configuration
Keep the target next to whatever else is specific to the provider, so the two can't get out of step:

```kotlin
enum class Provider(val target: SchemaTarget) {
    GEMINI(SchemaTarget.GEMINI),
    OPENAI(SchemaTarget.OPENAI),
}

val provider = Provider.valueOf(System.getenv("AI_PROVIDER") ?: "GEMINI")

val answer = client(provider).generate(prompt, schema = RecipeSchema[provider.target])
val recipe = Json.decodeFromString<Recipe>(answer)
```

The decoding is the same for every provider: the answer follows the schema of the same class.

## Passing the schemas of a class around
The generated objects don't share an interface. To write code that works for any root, pass a function reference:

```kotlin
suspend inline fun <reified T> Client.ask(prompt: String, schemaOf: (SchemaTarget) -> String): T {
    val answer = generate(prompt, schema = schemaOf(target))
    return Json.decodeFromString<T>(answer)
}

val recipe: Recipe = client.ask(prompt, RecipeSchema::get)
```

## Dialects that aren't generated
A root doesn't always have every dialect: the module can narrow them down with `jsonSchema.targets`, and a class with
`@SchemaRoot(targets)`. See [Choosing the dialects](../reference/targets.md#choosing-the-dialects).

- The constant of a dialect that isn't generated doesn't exist, so using it doesn't compile.
- `getOrNull(target)` returns `null`.
- `get(target)` throws an `IllegalArgumentException` that says where to add it:

  ```text
  RecipeSchema has no OPENAI schema, only GEMINI. Add openai to the jsonSchema.targets KSP option, or OPENAI to
  @SchemaRoot(targets = ...) on Recipe.
  ```

Use `getOrNull` when a missing dialect is expected, for example to skip a provider that can't take a class:

```kotlin
// Scores has a Map, so it's generated for Gemini only.
@SchemaRoot(targets = [SchemaTarget.GEMINI])
data class Scores(val byPlayer: Map<String, Int>)

val providers = Provider.entries.filter { ScoresSchema.getOrNull(it.target) != null }
```
