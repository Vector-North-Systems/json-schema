# Releasing
How versions, branches and tags work, and the steps to publish a release.

## Versions
The project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Until 1.0.0:
- Bump the **minor** version (`0.2.0`) for changes to the API or the generated schemas.
- Bump the **patch** version (`0.1.1`) for fixes that change neither.

`main` always carries the next version as a snapshot, for example `0.2.0-SNAPSHOT`, set in `gradle.properties`.
Snapshots are only published to the local Maven repository.

## Branches and tags
- `main` is always releasable. Changes land through short-lived branches and pull requests.
- Each release is an annotated tag on `main`, named `v<version>`, for example `v0.1.0`. The tag points at exactly
  what was published.
- Release branches are created only when needed: to fix a released version after `main` has moved on, branch
  `release/<major>.<minor>.x` (for example `release/0.1.x`) from the release tag, fix it there, and tag the patch
  release (`v0.1.1`) on that branch. Port the fix to `main` too.

## Release steps
1. Make sure `main` builds: `./gradlew clean build`.
2. Commit the release:
   - Set `version=<version>` in `gradle.properties`.
   - In `CHANGELOG.md`, rename `[Unreleased]` to `[<version>] - <YYYY-MM-DD>` and add a new empty `[Unreleased]`
     section above it.
   - Update the version in the README's Installation section.
   - Commit as `Release <version>`.
3. Tag and push:
   ```shell
   git tag -a v<version> -m "<version>"
   git push origin main v<version>
   ```
4. Publish the artifacts from the tagged commit.
5. Create a GitHub release from the tag, with the version's changelog section as notes.
6. Start the next version: set `version=<next>-SNAPSHOT` in `gradle.properties` and commit as
   `Prepare <next>-SNAPSHOT`.

## History
Amend or squash commits freely before pushing. Never rewrite history that's been pushed to `main` or included in a
tag.
