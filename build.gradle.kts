plugins {
    kotlin("jvm") version "2.4.20" apply false
    kotlin("plugin.serialization") version "2.4.20" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.11" apply false
}

// POM metadata shared by every published module.
subprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    url = "https://github.com/Vector-North-Systems/json-schema"
                    inceptionYear = "2026"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    scm {
                        url = "https://github.com/Vector-North-Systems/json-schema"
                        connection = "scm:git:git://github.com/Vector-North-Systems/json-schema.git"
                        developerConnection = "scm:git:ssh://git@github.com/Vector-North-Systems/json-schema.git"
                    }
                    developers {
                        developer {
                            name = "Eddie Lopez"
                            organization = "Vector North Systems"
                            organizationUrl = "https://vectornorthsystems.com"
                        }
                    }
                }
            }
        }
    }
}