package com.vectornorthsystems.samples

import com.vectornorthsystems.jsonschema.*
import com.vectornorthsystems.samples.providers.Model
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.OffsetDateTime

@Serializable
enum class Venue {
    ONLINE, ONSITE
}

@Serializable
@SchemaRoot
data class Conference(
    // A title without a description.
    @SchemaTitle("Name")
    val name: String,

    // The three string formats.
    @SchemaDescription("The date and the time when the doors open, with the UTC offset")
    @SchemaFormat(SchemaFormat.Format.DATE_TIME)
    val opensAt: String,

    @SchemaFormat(SchemaFormat.Format.DATE)
    val day: String,

    @SchemaDescription("When the keynote starts, in local time")
    @SchemaFormat(SchemaFormat.Format.TIME)
    val keynoteAt: String,

    // An enum that isn't nullable, so its values don't include null.
    val venue: Venue,

    // Bounds on an integer, written without a fraction: "minimum": 1.
    @SchemaMinimum(1.0)
    @SchemaMaximum(500.0)
    val capacity: Int,

    // Both bounds on an array.
    @SchemaMinItems(2)
    @SchemaMaxItems(4)
    val speakers: List<String>,
)

/**
 * Checks the annotations the other samples don't send to a provider: [SchemaFormat] with each format, [SchemaTitle],
 * [SchemaMinItems], and [SchemaMinimum] and [SchemaMaximum] on an integer. Also an enum that isn't nullable.
 *
 * The formats stay strings in the class, so the sample parses them to check the model followed each format.
 *
 * Run it with a Gemini API key:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="conference"
 */
suspend fun extractConference(model: Model) {
    println("The Conference class JSON schema is: ${ConferenceSchema[model.target]}")

    val input = """
        Extract the conference described below.

        KotlinConf Lisbon takes place on site on March 14, 2027. The doors open on that day at 9:00 in the morning,
        Lisbon time, which is UTC+00:00, and the keynote starts at 10:30. The room holds 350 people.
        The speakers are Ana Silva, João Costa and Maria Santos.
    """.trimIndent()

    val modelResponse = model.generate(input, ConferenceSchema[model.target])
    println("Model response: $modelResponse")

    val conference = Json.decodeFromString<Conference>(modelResponse)

    conference.name shouldContain "KotlinConf"

    // date-time: an RFC 3339 date and time with an offset, which fails to parse otherwise.
    val opensAt = OffsetDateTime.parse(conference.opensAt)
    opensAt.toLocalDate() shouldBe LocalDate.of(2027, 3, 14)
    opensAt.hour shouldBe 9

    // date: only the date.
    LocalDate.parse(conference.day) shouldBe LocalDate.of(2027, 3, 14)

    // time: only the time. Providers differ on the seconds and the offset, so only the start is checked.
    conference.keynoteAt.startsWith("10:30") shouldBe true

    conference.venue shouldBe Venue.ONSITE
    conference.capacity shouldBe 350
    conference.speakers.size shouldBe 3
}
