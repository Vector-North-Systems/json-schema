package com.vectornorthsystems.samples.providers

import com.vectornorthsystems.jsonschema.SchemaTarget
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * OpenAI, through the Responses API.
 *
 * @param apiKey The API key.
 * @param modelName The model that answers.
 */
class OpenAi(
    private val apiKey: String,
    private val modelName: String = "gpt-6-luna",
    val name: String = "response",
) : Model {
    private val client = HttpClient(Java)

    /**
     * Sends [input] with [schema] as the response format, and returns the model's answer, i.e.: the JSON that should match
     * the schema.
     */
    override suspend fun generate(input: String, schema: String): String {
        val request = buildJsonObject {
            put("model", modelName)
            put("input", input)
            putJsonObject("text") {
                putJsonObject("format") {
                    put("type", "json_schema")
                    put("name", name)
                    put("strict", true)
                    // Parsed, so the schema is sent as a JSON object rather than a quoted string.
                    put("schema", Json.parseToJsonElement(schema))
                }
            }
        }

        val response = client.post("https://api.openai.com/v1/responses") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request.toString())
        }

        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "OpenAI request failed: $body" }

        return outputText(Json.parseToJsonElement(body).jsonObject)
    }

    override val target: SchemaTarget
        get() = SchemaTarget.OPENAI

    override fun close() {
        client.close()
    }

    /**
     * Returns the model's text from a Responses API response. Fails when the response is incomplete, or when the
     * model refused instead of answering.
     */
    internal fun outputText(response: JsonObject): String {
        // "incomplete" when the output was cut short, e.g. by max_output_tokens: the JSON would be truncated.
        val status = response["status"]?.jsonPrimitive?.content
        check(status == "completed") {
            val reason = response["incomplete_details"]?.jsonObject?.get("reason")?.jsonPrimitive?.content
            "OpenAI response is $status${reason?.let { " ($it)" }.orEmpty()}: $response"
        }

        // The answer is in the message items of `output`; reasoning models put other items, like reasoning, before it.
        val content = response.getValue("output").jsonArray
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.content == "message" }
            .flatMap { it["content"]?.jsonArray.orEmpty() }
            .map { it.jsonObject }

        // A refusal replaces the answer, so there's no JSON to decode.
        content.firstOrNull { it["type"]?.jsonPrimitive?.content == "refusal" }?.let {
            error("OpenAI refused: ${it["refusal"]?.jsonPrimitive?.content}")
        }

        return content
            .filter { it["type"]?.jsonPrimitive?.content == "output_text" }
            .joinToString("") { it.getValue("text").jsonPrimitive.content }
            .ifEmpty { error("OpenAI response has no output text: $response") }
    }
}