package com.vectornorthsystems.samples

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Gemini, through the Interactions API.
 *
 * @param apiKey The Gemini API key.
 * @param modelName The Gemini model that answers.
 */
class Gemini(
    private val apiKey: String,
    private val modelName: String = "gemini-3.5-flash-lite",
) : Model {
    private val client = HttpClient(Java)

    override suspend fun generate(input: String, schema: String): String {
        val request = buildJsonObject {
            put("model", modelName)
            put("input", input)
            putJsonObject("response_format") {
                put("type", "text")
                put("mime_type", "application/json")
                // Parsed, so the schema is sent as a JSON object rather than a quoted string.
                put("schema", Json.parseToJsonElement(schema))
            }
        }

        val response = client.post("https://generativelanguage.googleapis.com/v1beta/interactions") {
            header("x-goog-api-key", apiKey)
            // Pins the request and response format of the Interactions API.
            header("Api-Revision", "2026-05-20")
            contentType(ContentType.Application.Json)
            setBody(request.toString())
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "Gemini request failed: $body" }

        // The answer is the text content of the model_output steps.
        return Json.parseToJsonElement(body).jsonObject
            .getValue("steps").jsonArray
            .asSequence()
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.content == "model_output" }
            .flatMap { it["content"]?.jsonArray.orEmpty() }
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.content == "text" }
            .joinToString("") { it.getValue("text").jsonPrimitive.content }
    }

    override fun close() = client.close()
}
