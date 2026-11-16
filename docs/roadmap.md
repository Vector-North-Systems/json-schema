# Roadmap

Planned, roughly in order. Features with design decisions to make have a proposal in [`proposals`](proposals), linked
below.

1. **Serialization names.** Use the names the decoder expects and leave out `@Transient` properties, starting with
   kotlinx.serialization's `@SerialName`, then Moshi's `@Json(name)`, Gson's `@SerializedName` and Jackson's
   `@JsonProperty`. Decoding tests will verify the missing-property behavior in
   [Required and nullable properties](concepts.md#required-and-nullable-properties) for each supported decoder.
2. **Sealed classes through `anyOf`.** Each subclass becomes an option, with a discriminator property so the answer
   can be decoded. Later, `@SchemaSubtypes` for interfaces and abstract classes, whose subclasses the compiler doesn't
   know.
3. **Java types.** Boxed primitives, `java.util` collections and maps, `java.time` types (as strings with a `format`),
   `UUID`, `BigDecimal` and `BigInteger`.
4. **More dialects.** Anthropic, Mistral, xAI, DeepSeek and plain JSON Schema. All use JSON Schema for structured
   output, but each accepts a different subset.
5. **More constraints.** String patterns, more string formats, `multipleOf` and exclusive bounds, in the dialects that
   support them.
6. **Property order.** Gemini 2.5 and later keep the schema's key order in the output, so declaration order shapes
   generation (e.g. reasoning before answer). The library's ordering guarantee will be documented and tested, including
   inherited properties.
7. **Enums for numbers and strings.** `@SchemaEnum` restricts an `Int`, `Double` or `String` property to a set of
   values, without declaring an enum class. See [proposal 0001](proposals/0001-schema-enum.md).
8. **Titles and descriptions on classes.** Describe a type once, on the class, instead of on every property that uses
   it. Also describes the root schema and enum classes. See [proposal 0002](proposals/0002-class-descriptions.md).
9. **Kotlin Multiplatform.** Multiplatform annotations, so schema classes can live in `commonMain`. See
   [proposal 0003](proposals/0003-kotlin-multiplatform.md).
10. **Readable `$defs` names.** For generics: names like `Box_String` instead of `Box2`, which makes the schema easier
    to read and debug. See [#11](https://github.com/Vector-North-Systems/json-schema/issues/11).

## Known issues
- **Classes from compiled libraries.** Required properties come from constructor defaults, which haven't been tested
  for classes from another module or a library jar. If KSP can't read their defaults, every constructor parameter is
  required. Decoding still works, but the AI model can't leave out properties that have a default. Until this is
  verified, check the generated schema, or describe these classes with your own types.
  See [#9](https://github.com/Vector-North-Systems/json-schema/issues/9).
- **Java classes.** They aren't mapped yet, so they're described by their private fields, or fail the build.
- **Provider limits.** A provider's limits on the size of a schema, like how deep objects nest, aren't checked. A
  schema over a limit is generated, and rejected by the provider.
- See [all open bugs](https://github.com/Vector-North-Systems/json-schema/issues?q=is%3Aissue+state%3Aopen+type%3ABug).
