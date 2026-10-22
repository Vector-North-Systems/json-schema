package com.vectornorthsystems.samples

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot
import io.ktor.client.*
import io.ktor.client.engine.java.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
@SchemaRoot
data class Publication(
    val title: String,
    val author: Person,
    // Nullable but required: the model has to answer null explicitly when there's no editor.
    val editor: Person?,
    val sections: List<Section>,
)

@Serializable
data class Person(
    val name: String,
    @SchemaDescription("The person's organization or university, if mentioned")
    val affiliation: String? = null,
)

@Serializable
data class Section(
    @SchemaDescription("The section's title, without its number")
    val heading: String,
    val subsections: List<Section> = emptyList(),
    val reviewer: Person? = null,
)

/**
 * An example of the whole round trip with Gemini structured output.
 * Exercises `$defs` beyond the root `"$ref": "#"` that Gemini's docs show, and is `@Serializable` so we can decode the response:
 *
 * Run it with a Gemini API key:
 *     GEMINI_API_KEY=... ./gradlew :json-schema-tests:run
 *
 * - [Person] isn't recursive, but is used in three places, so it's defined once in `$defs`. One of those places is
 *   inside another definition ([Section.reviewer]), so definitions reference each other.
 * - [Section] contains itself, so it's defined in `$defs` and references itself from there.
 * - [Publication.editor] and [Section.reviewer] are nullable references, written as `anyOf` with `null`.
 */
suspend fun extractPublication() {
    println("The com.vectornorthsystems.samples.Publication class JSON schema is: ${getPublicationSchema()}")

    val input = """
            Extract the publication described below.
 
            Kotlin in Practice, by Ana Silva of Vector North Systems. The book has no editor.
            Table of contents:
            1 Basics
              1.1 Types
              1.2 Null safety (reviewed by João Costa, University of Lisbon)
            2 Coroutines
              2.1 Scopes
                2.1.1 Structured concurrency
              2.2 Flows
        """.trimIndent()

    val publication = HttpClient(Java).use {
        val output = it.generate(input, getPublicationSchema())
        println("Model output: $output")

        // The default Json is strict: an unknown or missing required property fails here.
        Json.decodeFromString<Publication>(output)
    }

    // Root properties, including the explicit null through anyOf.
    publication.author.name shouldBe "Ana Silva"
    publication.editor.shouldBeNull()

    // com.vectornorthsystems.samples.Section references itself from $defs, three levels deep.
    publication.sections.size shouldBe 2
    val (basics, coroutines) = publication.sections
    basics.subsections.size shouldBe 2
    coroutines.subsections.size shouldBe 2
    val scopes = coroutines.subsections.first()
    scopes.subsections.size shouldBe 1
    scopes.subsections.single().heading shouldContain "Structured concurrency"

    // com.vectornorthsystems.samples.Person referenced from inside the com.vectornorthsystems.samples.Section definition.
    val reviewer = checkNotNull(basics.subsections.last().reviewer)
    reviewer.name shouldContain "João Costa"
    basics.subsections.first().reviewer.shouldBeNull()
}
