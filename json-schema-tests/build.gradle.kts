plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    application
}

application {
    mainClass.set("MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":json-schema-annotations"))
    implementation("io.ktor:ktor-client-core:3.6.0")
    implementation("io.ktor:ktor-client-java:3.6.0")
    // Builds the Gemini request and decodes the reply in Main.kt; the tests also parse the generated schemas with it.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    ksp(project(":json-schema-ksp"))
    testImplementation(kotlin("test"))
}

// Sets the module's dialects, as a user would, so the option is read by a real build and not only by unit tests.
// An unknown or empty list here fails the build.
ksp {
    arg("jsonSchema.targets", "gemini")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}