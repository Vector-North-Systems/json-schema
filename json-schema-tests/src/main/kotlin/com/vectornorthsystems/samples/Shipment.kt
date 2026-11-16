package com.vectornorthsystems.samples

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTitle
import com.vectornorthsystems.samples.providers.Model
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PostalAddress(
    val street: String,
    val city: String,
    val country: String,
)

/**
 * [PostalAddress] is used four times, so it's defined once in `$defs`, and every use is a `$ref`. Each use has a
 * title or a description, which is written in the same JSON object as the reference, or around it in the dialects
 * that reject keywords next to a `$ref`.
 */
@Serializable
@SchemaRoot
data class Shipment(
    // A description next to a $ref.
    @SchemaDescription("Where the parcel is delivered")
    val destination: PostalAddress,

    // A title and a description next to a $ref.
    @SchemaTitle("Sender")
    @SchemaDescription("Where the parcel is sent from")
    val origin: PostalAddress,

    // A description next to the anyOf of a nullable $ref.
    @SchemaDescription("Where the parcel goes back to if it can't be delivered, or null if the text doesn't say")
    val returnTo: PostalAddress?,

    // A description next to an array, whose items are a $ref. No keyword sits next to this reference.
    @SchemaDescription("The addresses the parcel passes through on the way, in order")
    val stops: List<PostalAddress>,
)

/**
 * Checks that a provider accepts a reference with a title or a description. In the `GEMINI` dialect they're next to
 * the `$ref`:
 *
 * ```json
 * "destination": {"description": "Where the parcel is delivered", "$ref": "#/$defs/PostalAddress"}
 * ```
 *
 * JSON Schema allows it since draft 2019-09, but OpenAI's strict mode rejects the schema, with
 * `$ref cannot have keywords {'description'}`. So the `OPENAI` dialect wraps the reference:
 *
 * ```json
 * "destination": {"description": "Where the parcel is delivered", "anyOf": [{"$ref": "#/$defs/PostalAddress"}]}
 * ```
 *
 * Run it with each provider:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="shipment"
 *   MODEL=openai OPENAI_API_KEY=... ./gradlew :json-schema-tests:run --args="shipment"
 */
suspend fun extractShipment(model: Model) {
    println("The Shipment class JSON schema is: ${ShipmentSchema[model.target]}")

    val input = """
        Extract the shipment described below.

        A parcel is sent from 12 Rua Augusta, Lisbon, Portugal to 5 Calle Mayor, Madrid, Spain.
        On the way it passes through a depot at 80 Avenida da Liberdade, Porto, Portugal, and then through one at
        3 Paseo de Zorrilla, Valladolid, Spain.
        The text doesn't say where the parcel goes back to.
    """.trimIndent()

    val modelResponse = model.generate(input, ShipmentSchema[model.target])
    println("Model response: $modelResponse")

    // The default Json is strict: an unknown or missing required property fails here.
    val shipment = Json.decodeFromString<Shipment>(modelResponse)

    // The described references decode like any other.
    shipment.origin.city shouldBe "Lisbon"
    shipment.destination.city shouldBe "Madrid"
    shipment.destination.country shouldBe "Spain"

    // The described nullable reference takes an explicit null.
    shipment.returnTo.shouldBeNull()

    // The references inside the described array.
    shipment.stops.map { it.city } shouldBe listOf("Porto", "Valladolid")
}
