plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    // Coverage of the unit tests.
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":json-schema-annotations"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk-jvm:1.14.11")
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral()
    // Signing needs the in-memory key properties, so local builds and publishToMavenLocal work without them.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name = "Kotlin JSON Schema Processor"
        description = "KSP processor that generates JSON Schemas from annotated Kotlin classes."
    }
}

tasks.test {
    useJUnitPlatform()
}