# Changelog
All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Until 1.0.0, minor versions may change the generated schemas
or the API (annotations).

## [Unreleased]

## [0.3.0] - 2026-10-29

### Added
- Dialects: `SchemaTarget` lists the subsets of JSON Schema a schema can be generated for, `GEMINI` for now. Each
  `@SchemaRoot` class gets one schema per dialect.
- The `jsonSchema.targets` KSP argument, a comma separated list of the dialects to generate in the module. All of them
  are generated without it. An unknown dialect or an empty list fails the build.
- `@SchemaRoot(targets)`, to set the dialects of a class, overriding the module's.
- A warning for unknown `jsonSchema.*` KSP arguments, which are likely typos.

### Changed
- The schemas are in a generated object instead of a function: `{ClassName}Schema`, with a constant per dialect, and
  `get(target)` and `getOrNull(target)` to pick one at runtime. This breaks code that calls the generated functions:
  replace `getComplexSchema()` with `ComplexSchema.GEMINI`.
- `@SchemaRoot(functionName)` is renamed to `@SchemaRoot(name)`, and names the object. This breaks the build of code
  that sets it: rename the argument, and choose a name for an object, e.g. `ComplexJsonSchema`.

### Removed
- The `@SchemaPrefixItems` annotation, deprecated in 0.2.0. This breaks the build of code that still uses it: remove
  the annotation, or replace the array with a data class whose properties have the types you need.

### Fixed
- Nullable enums list `null` among their `enum` values, not only in their `type`, so JSON Schema validators and AI
  models accept an explicit `null`. Before, the schema wasn't compliant.
  See [#10](https://github.com/Vector-North-Systems/json-schema/issues/10).

## [0.2.0] - 2026-10-27

### Fixed
- The Schema Expansion issue from the known issues.
- Value classes are described by the type they wrap, matching how kotlinx.serialization encodes them, instead of as
  an object with a `value` property.

### Added
- Any class used twice now moves to $defs, not only recursive ones. This yields smaller schemas that save tokens.

### Changed
- The generated function is `internal` when the `@SchemaRoot` class is internal or private, and public otherwise,
  instead of always public. A private class gets an internal function because the function is generated in its own
  file, where a private one could not be called.

### Deprecated
- The `@SchemaPrefixItems` annotation is deprecated and will be removed before 1.0. Models follow `prefixItems` and
  produce arrays of mixed types, which a Kotlin collection can't hold, so the response fails to decode.

## [0.1.3] - 2026-10-18
First release, published as `com.vectornorthsystems:json-schema-annotations` and `com.vectornorthsystems:json-schema-ksp`.

### Added
- `@SchemaRoot`, which generates a function in the class's package that returns the class's JSON Schema as a
  string. The function is `get${ClassName}Schema()` by default, or the name set by `@SchemaRoot(functionName)`.
- Schemas for primitives, strings, enums, unsigned numbers, `Char`, arrays, collections, maps with `String` keys and
  nested classes, following the subset of JSON Schema that Gemini supports.
- Nullable types allow `null`. Primary constructor parameters without a default are required, and `@SchemaRequired`
  makes a property required even when it has a default.
- Type parameters resolved per use, recursive classes through `$ref` and `$defs`, and a nesting limit of 32 objects.
- Annotations for `title`, `description`, `format`, `minimum`, `maximum`, `minItems`, `maxItems` and `prefixItems`.
- Build errors, pointing at the declaration, for unsupported types, misplaced annotations, invalid function names and
  function names already generated in the package.
