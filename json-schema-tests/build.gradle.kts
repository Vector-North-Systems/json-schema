plugins {
    kotlin("jvm")
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

    ksp(project(":json-schema-ksp"))
    testImplementation(kotlin("test"))
    // Parses the generated schemas, no serialization plugin needed.
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}