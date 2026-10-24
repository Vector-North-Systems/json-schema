# 0003: Kotlin Multiplatform

**Status:** Draft

## Motivation
`json-schema-annotations` is a JVM library (`kotlin("jvm")`), so code in a Kotlin Multiplatform project's `commonMain`
can't use it. Many apps that call Gemini are Android and iOS apps sharing their models in `commonMain`, which is
exactly where schema classes belong: one `Recipe` class, one schema, used on every platform.

## Proposal
Publish the annotations as a multiplatform library, for every target they compile to. The processor stays a JVM
library, since KSP runs it on the JVM at build time on every platform.

The generated code is already plain common Kotlin, a function returning a string literal, so it compiles for every
target unchanged. Users run the processor once, on the common metadata, and its output becomes part of `commonMain`:

```kotlin
kotlin {
    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        dependencies { implementation("com.vectornorthsystems:json-schema-annotations:<version>") }
    }
}

dependencies {
    add("kspCommonMainMetadata", "com.vectornorthsystems:json-schema-ksp:<version>")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
}
```

The `srcDir` line makes the IDE index the generated code, and the task dependency makes every compilation wait for
it. The exact wiring varies between KSP versions, so the README will show the setup verified by the sample module
(below).

### Scope
- The processor sees only `commonMain` declarations. Schema classes in platform source sets aren't processed.
- Java types (`java.time`, `UUID`, ...) don't exist in common code, so the planned Java mappings never apply there.

### Build errors
None new.

## Decoding
kotlinx.serialization is multiplatform, so the code-first round trip works in `commonMain` as it does on the JVM.
Moshi, Jackson and Gson are JVM-only, so common code can only use kotlinx.serialization.

## Provider support
Unaffected: the schemas are identical on every platform.

## Implementation notes
- Convert `json-schema-annotations` to `kotlin("multiplatform")`. It only declares annotations and uses `KClass`, both
  available in common code.
- **Targets:** JVM, JS, Wasm, and the native targets: Apple, Linux, Windows, Android Native. Apple targets must be
  built on macOS, so publishing needs a macOS runner.
- **Existing JVM users:** Gradle resolves the JVM variant from the same coordinates. Maven users need the
  `json-schema-annotations-jvm` artifact, so the change goes in the changelog.
- **Kotlin versions:** klibs, the format of non-JVM Kotlin libraries, are less forgiving across Kotlin versions than
  JVM bytecode. The README should state the minimum Kotlin version for multiplatform users.
- **Sample module:** add a small multiplatform module to the repo, built in CI, with a JVM target and a native or JS
  target that builds on Linux runners. It verifies the setup above and calls a generated function from `commonMain`.

## Alternatives considered
- **Processing each target separately** (`kspJvm`, `kspIosArm64`, ...). Each target gets its own copy of the generated
  code, which `commonMain` can't call. Running both this and the common processing declares the functions twice.
- **Generating `expect`/`actual` declarations.** Only needed when generated code differs per platform, which it
  doesn't.
- **A Gradle plugin that does the setup,** applying KSP, adding the dependencies and wiring the tasks. Simpler for
  users, but another artifact to maintain, and it would have to track KSP's changes. Worth reconsidering if the
  manual setup proves error-prone.
- **Staying JVM-only.** Multiplatform users would have to keep their schema classes in a JVM source set, which defeats
  sharing them.

## Open questions
- Does the processor see annotations on schema classes from another multiplatform module, through klib metadata? The
  sample module should include a case of this.
- Should the processor warn when it runs on a platform compilation in a multiplatform project, to catch the
  duplicate-declaration mistake?
