plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":json-schema-annotations"))
    ksp(project(":json-schema-ksp"))
    testImplementation(kotlin("test"))
}

// The option this module checks, set as a user would, so a real build validates it.
// An unknown or empty list here fails the build. See README.md.
ksp {
    arg("jsonSchema.targets", "gemini")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
