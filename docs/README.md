# Documentation

Start with the [README](../README.md) for what the library is for and a first example.

## Learn
- [Getting started](getting-started.md): set up a project, generate a first schema and send a first request.
- [Concepts](concepts.md): code-first schemas, dialects, and required versus nullable properties.

## Guides
One per provider, with the same sections: what the dialect generates, sending the schema, reading the answer, and
the things to watch out for.

- [Gemini](guides/gemini.md)
- [OpenAI](guides/openai.md)
- [Runtime targets](guides/runtime-targets.md): choosing the provider at runtime.

## Reference
- [Types](reference/types.md): how each Kotlin type is described, and which properties are included.
- [Annotations](reference/annotations.md): every annotation, the types it applies to and the keyword it writes.
- [Dialects](reference/targets.md): the targets, how to choose them, and how they differ.
- [Errors](reference/errors.md): every build error, its cause and its fix.

## Project
- [Roadmap](roadmap.md): what's planned, and the known issues.
- [Proposals](proposals): designs for features that have decisions to make.

## How the pages are organized
The pages are organized so a new dialect adds to them, without moving anything:

| A new dialect adds                                           | Where                                                    |
|--------------------------------------------------------------|----------------------------------------------------------|
| A row in the supported dialects table                        | [README](../README.md#supported-dialects)                |
| A guide, with the same sections as the others                | `guides/<provider>.md`, linked from this page            |
| A row in the targets table, and a column in the comparison   | [Dialects](reference/targets.md)                         |
| A column for whether each annotation's keyword is written    | [Annotations](reference/annotations.md)                  |
| A column in the required and nullable table                  | [Concepts](concepts.md#required-and-nullable-properties) |
| A row per type it doesn't support                            | [Types](reference/types.md#types-a-dialect-rejects)      |
| Its own build errors                                         | [Errors](reference/errors.md#dialect-errors)             |

Rules that are the same in every dialect are written once, in the reference. Only what differs is in the dialect
columns and in the guides.
