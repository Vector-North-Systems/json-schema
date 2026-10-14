plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "json-schema"
include("json-schema-ksp")
include("json-schema-annotations")
include("json-schema-tests")