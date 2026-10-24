# 0001: `@SchemaEnum`

**Status:** Draft

## Motivation
Today the schema only gets an `enum` from Kotlin enum classes, written as strings with the entry names. Gemini also
accepts `enum` on `number` and `integer` values, but there's no way to produce one. There's also no way to restrict a
plain `String` to a few values without declaring an enum class.

```kotlin
val cores: Int          // Should be 1, 2, 4 or 8
val priority: String    // Should be "low", "medium" or "high"
```

## Proposal
A property annotation listing the allowed values. The values are strings, and the processor interprets them according
to the property's type:

```kotlin
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class SchemaEnum(vararg val values: String)
```

```kotlin
@SchemaEnum("1", "2", "4", "8")
val cores: Int

@SchemaEnum("low", "medium", "high")
val priority: String
```

Generates:

```json
"cores": {"type": "integer", "enum": [1, 2, 4, 8]},
"priority": {"type": "string", "enum": ["low", "medium", "high"]}
```

| Property type      | Values must be                   | Schema                       |
|--------------------|----------------------------------|------------------------------|
| `Int`, `Long`      | Whole numbers in the type's range | `integer` with numeric `enum` |
| `Float`, `Double`  | Numbers                          | `number` with numeric `enum`  |
| `String`           | Any strings                      | `string` with string `enum`   |

### Build errors
- `@SchemaEnum` on any other type, including enum classes, which already have an `enum`.
- A value that doesn't parse as the property's type, or is out of range for `Int`.
- No values, or duplicate values.
- A value outside the bounds of `@SchemaMinimum` or `@SchemaMaximum` on the same property.

### Nullable properties
A nullable property gets `null` added to its `enum`, the same fix planned for nullable enum classes.

## Decoding
The property is still a plain `Int` or `String`, so kotlinx.serialization, Moshi and Jackson decode it as before. The
schema constrains what the model produces; the decoder doesn't check the values, and doesn't need to.

## Provider support
- **Gemini:** `enum` is part of its subset for strings and numbers. To verify with a live test for an `integer` and a
  `number` enum before release.
- **OpenAI and Anthropic:** to check when their transformers are added.

## Implementation notes
- `PrimitiveNode.enum` becomes a list of typed values, not `List<String>`, so the writer can emit numbers with
  `toJsonNumber()`.
- `NumberProperties.ENUM` is already declared in `Keywords.kt`.

## Alternatives considered
- **Typed annotations,** e.g. `@SchemaIntEnum(vararg val values: Long)` and `@SchemaNumberEnum(vararg val values: Double)`.
  The compiler would check the value types, but it means three annotations for one concept. With a single annotation,
  the processor still reports mistakes at build time.
- **Enum classes with associated values.** Covered in its own section below.

### Enum classes with associated values
Kotlin enum entries can carry values, which would make the enum class the single source of the allowed numbers:

```kotlin
enum class Cores(val count: Int) { ONE(1), TWO(2), FOUR(4), EIGHT(8) }
```

The processor would write `{"type": "integer", "enum": [1, 2, 4, 8]}`. It has to know which property holds the
value, by a naming convention or an annotation such as `@SchemaEnumValue("count")`, and it has to read each entry's
value. Both steps run into problems:

- **Reading the values.** KSP exposes declarations, not expressions, so it can't see the `1` in `ONE(1)`. A function
  returning the value is worse, since the processor can't run code. The workarounds all have costs:
    - Parsing the source text of the enum is possible, but fragile: values like `ONE(BASE * 2)` or constants can't be
      resolved, and enums from libraries have no source.
    - A Kotlin compiler plugin could read constant arguments, but it's far heavier than KSP and is tied to compiler
      versions.
    - A per-entry annotation, such as `@SchemaValue("1") ONE(1)`, works, but repeats every value.
- **Decoding.** kotlinx.serialization, Moshi and Gson decode enums by name, so a response of `4` fails to decode
  without a custom serializer, which the processor can't see. Jackson is the exception: it can decode enums through
  a property annotated with `@JsonValue`.

The per-entry annotation is the only variant that works at compile time. It means writing every value twice and
keeping a custom serializer in sync with both, which breaks the library's main promise: the classes are the single
source for both the schema and the decoding. Not pursued for now. Worth reconsidering if Jackson support is added,
since `@JsonValue` solves the decoding side.

## Open questions
- Should `@SchemaEnum` on a `List<Int>` or `List<String>` constrain the elements? Not in this proposal; it could follow
  the same rules applied to `items`.
