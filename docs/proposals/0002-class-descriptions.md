# 0002: Titles and descriptions on classes

**Status:** Draft

## Motivation
`@SchemaTitle` and `@SchemaDescription` only apply to properties. A nested type used in several places needs its
description repeated on each property, and the root schema and enum classes can't be described at all:

```kotlin
data class Recipe(
    @SchemaDescription("An ingredient, with its quantity in grams")
    val main: Ingredient,

    @SchemaDescription("An ingredient, with its quantity in grams")
    val extras: List<Ingredient>,
)
```

The model reads these descriptions, so describing what a type *is* once, and what each property *is for* separately,
gives it better guidance with less repetition.

## Proposal
Add `AnnotationTarget.CLASS` to `@SchemaTitle` and `@SchemaDescription`:

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class SchemaDescription(val text: String)
```

```kotlin
@SchemaDescription("An ingredient, with its quantity in grams")
data class Ingredient(val name: String, val grams: Int)

@SchemaDescription("How hard the recipe is to make")
enum class Difficulty { EASY, MEDIUM, HARD }

@SchemaRoot
@SchemaDescription("A recipe extracted from the user's text")
data class Recipe(
    val main: Ingredient,
    @SchemaDescription("Optional ingredients the recipe suggests")
    val extras: List<Ingredient>,
    val difficulty: Difficulty,
)
```

The class's title and description are written on the type's own schema, wherever that is:

| Where the type is written         | Class title and description go                          |
|-----------------------------------|---------------------------------------------------------|
| The root                          | At the top level of the schema                          |
| A `$defs` entry (shared or recursive) | On the entry, next to its `type` and `properties`    |
| Inline in a property              | On the inline schema, unless the property has its own (below) |
| Enum class                        | On the `string` schema with the `enum`                  |

### When a class and a property both have one
- **Shared or recursive types:** both are kept. The property's goes next to its `$ref`, and the class's on the
  `$defs` entry. The model sees what the type is, and what this property is for.
- **Inline types:** the property's and the class's would land in the same JSON object, so the property's wins. It's
  the more specific of the two.

For the `Recipe` above, `Ingredient` is used twice, so it's in `$defs`:

```json
"main": {"$ref": "#/$defs/Ingredient"},
"extras": {"description": "Optional ingredients the recipe suggests", "type": "array", "items": {"$ref": "#/$defs/Ingredient"}},
...
"$defs": {
  "Ingredient": {"description": "An ingredient, with its quantity in grams", "type": "object", ...}
}
```

### Build errors
None new. As with properties, the annotations take plain text.

## Decoding
Titles and descriptions are documentation for the model. Decoders ignore them.

## Provider support
- **Gemini:** `title` and `description` are part of its subset, as they are for properties today.
- **OpenAI and Anthropic:** to check when their transformers are added.

## Implementation notes
- `ObjectNode`, and the `PrimitiveNode` of enum classes, get `title` and `description`, read in `objectSchemaFor` and
  for `ENUM_CLASS` in `nonNullSchemaFor`.
- `PropertyNode` writes its title and description before the declaration's schema, so for inline types the writer
  must skip the class's when the property has its own, or the JSON object gets duplicate keys.
- The schema test needs one case per row of the table, and one for each precedence rule.

## Alternatives considered
- **Separate annotations for classes,** e.g. `@SchemaTypeDescription`. More API for the same concept, and Kotlin
  already distinguishes the targets.
- **Joining both texts for inline types,** e.g. "Optional ingredients the recipe suggests. An ingredient, with its
  quantity in grams." Keeps both, but the joined text reads awkwardly and the user doesn't control it.
- **Moving every described class to `$defs`,** so both texts always have their own place. Consistent, but adds a
  `$ref` for every described type, which makes the schema harder to read and depends on how well providers handle
  `$defs`.
- **Reading KDoc comments as descriptions.** KSP exposes `docString`, so no annotation would be needed. But KDoc is
  written for developers: it mentions implementation details, uses `[links]` and Markdown, and an edited comment would
  silently change the model's behavior. Could come later as an opt-in.

## Open questions
- Is "the property's wins" the right rule for inline types, or would users expect the class's to be kept too?
