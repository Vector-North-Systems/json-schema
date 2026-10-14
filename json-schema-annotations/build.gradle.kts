plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral()
    // Signing needs the in-memory key properties, so local builds and publishToMavenLocal work without them.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name = "Kotlin JSON Schema Annotations"
        description = "Annotations that mark Kotlin classes for JSON Schema generation."
    }
}

tasks.test {
    useJUnitPlatform()
}