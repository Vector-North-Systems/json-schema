# Contributing
Thanks for your interest in improving the project! Bug reports, feature ideas and pull requests are all welcome.

## Reporting bugs and requesting features
Open an issue. For a bug, include the annotated class, the schema you expected and the one generated (or the build
error), plus your Kotlin and KSP versions. Check the [pending features](README.md#pendingpossible-next-features) first,
since some are already planned.

For larger changes, open an issue to discuss the approach before writing the code.

## Project structure
| Module                    | Contents                                                                    |
|---------------------------|-----------------------------------------------------------------------------|
| `json-schema-annotations` | The annotations users apply, in `com.vectornorthsystems.jsonschema`.        |
| `json-schema-ksp`         | The KSP processor that builds and writes the schemas.                       |
| `json-schema-tests`       | Fixtures, tests and a playground. Not published.                            |

## Building
You need JDK 17 or later.

```shell
./gradlew build
```

To try your changes in another project, publish them to the local Maven repository and add `mavenLocal()` to that
project's repositories:

```shell
./gradlew publishToMavenLocal
```

## Playground
`json-schema-tests/src/main/kotlin/Main.kt` holds a `Playground` class. Edit it and run `main()` to see the schema it
generates. Keep it in the root package: it also checks that classes without a package compile.

## Tests
The processor runs when `json-schema-tests` builds, so tests work through fixtures:
1. Add a `@SchemaRoot` class to `json-schema-tests/src/main/kotlin/com/vectornorthsystems/fixtures/`.
2. In `SchemaTest`, compare the generated function's output with the expected JSON. `assertSchema` parses both, so
   formatting and property order don't matter. If the latter grows, we can split it later.

Errors fail the build, so they can't be fixtures. Check them with a temporary fixture, confirm the message and the
location in the build output, and delete it before committing.

The examples in the README are copied in `fixtures/readme/Readme.kt` and checked by `ReadmeTest`. Update both together.

## Branches and pull requests
`main` is always releasable. Work on a short-lived branch from `main`, for example `fix/nested-defs` or
`feature/any-of`, and open a pull request to `main`. Keep each pull request to one change, and make sure
`./gradlew build` passes. Releases are tagged on `main`; see [RELEASING.md](RELEASING.md).

## Guidelines
- Generated schemas follow the
  [subset of JSON Schema that Gemini supports](https://ai.google.dev/gemini-api/docs/structured-output#json-schema-support).
  Check new keywords against it.
- Add a fixture and a test for every feature or fix, and document user-facing changes in the README.
- Add an entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md).
- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

## License
By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE.txt).
