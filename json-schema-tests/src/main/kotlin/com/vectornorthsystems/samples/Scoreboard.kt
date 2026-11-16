package com.vectornorthsystems.samples

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.samples.providers.Model
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A class with maps, which are objects with arbitrary keys: `"additionalProperties"` describes their values.
 *
 * OpenAI's strict mode can't express a map, so the class is only generated for Gemini. Without [SchemaRoot.targets],
 * the maps would fail the build of this module.
 */
@Serializable
@SchemaRoot(targets = [SchemaTarget.GEMINI])
data class Scoreboard(
    val game: String,

    // A map of integers.
    @SchemaDescription("The points of each player, by the player's first name")
    val pointsByPlayer: Map<String, Int>,

    // A map of arrays.
    @SchemaDescription("The awards of each player who got one, by the player's first name")
    val awardsByPlayer: Map<String, List<String>>,
)

/**
 * Checks maps with a provider, which the other samples don't send. It runs with the providers [Scoreboard] is
 * generated for, and is skipped with the others.
 *
 * Run it with a Gemini API key:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="scoreboard"
 */
suspend fun extractScoreboard(model: Model) {
    // A dialect that isn't generated for the class has no schema to send.
    val schema = ScoreboardSchema.getOrNull(model.target)
    if (schema == null) {
        println("Skipped: Scoreboard has maps, which the ${model.target} dialect doesn't support.")
        return
    }
    println("The Scoreboard class JSON schema is: $schema")

    val input = """
        Extract the scoreboard described below.

        In last night's game of darts, Ana scored 301 points, João scored 287 and Maria scored 244.
        Ana got the awards "Winner" and "Highest checkout". Maria got the award "Most improved".
        João got no award.
    """.trimIndent()

    val modelResponse = model.generate(input, schema)
    println("Model response: $modelResponse")

    val scoreboard = Json.decodeFromString<Scoreboard>(modelResponse)

    // The keys come from the text, not from the schema.
    scoreboard.pointsByPlayer shouldBe mapOf("Ana" to 301, "João" to 287, "Maria" to 244)
    scoreboard.awardsByPlayer["Ana"]?.size shouldBe 2
    scoreboard.awardsByPlayer["Maria"] shouldBe listOf("Most improved")
    // A player without awards may be left out, or have an empty list.
    scoreboard.awardsByPlayer["João"].orEmpty() shouldBe emptyList()
}
