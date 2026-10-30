# ksp-dialect-gemini

Checks that a real Gradle build using the `jsonSchema.targets` KSP option works as expected. And this particular
submodule should build only the `GEMINI` dialect.

With this, we expect to verify:
- An unknown or empty value fails the build, so if this module builds, the option was accepted.
- The tests check the dialects the option lists are generated (unless overridden, of course). Once `SchemaTarget` has a
second dialect, it should also check the ones it leaves out aren't generated (Coming soon).

## Adding a module for another option

Create a sibling module per option and value, named `ksp-{option}-{value}`, such as `ksp-dialect-openai`:

1. Copy this module's `build.gradle.kts`, and change the `ksp { arg(...) }` it checks.
2. Include it in `settings.gradle.kts`, as `include("json-schema-tests:ksp-{option}-{value}")` (Done by IntelliJ automatically).
3. Add a fixture root in `src/main/kotlin`, in a package named after the module, and tests in `src/test/kotlin` that
   check the option's effect on the generated object.
4. Add a README like this one, saying what the module checks.
