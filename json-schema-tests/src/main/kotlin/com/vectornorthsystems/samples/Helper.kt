package com.vectornorthsystems.samples

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Sends [input] to Gemini with [schema] as the response format, and returns the model's answer, i.e.: the JSON that matches the
 * schema.
 */
suspend fun HttpClient.generate(input: String, schema: String): String {
    val request = buildJsonObject {
        put(
            "model", listOf(
                "gemini-3.8-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.6-flash"
            ).random()
        )
        put("input", input)
        putJsonObject("response_format") {
            put("type", "text")
            put("mime_type", "application/json")
            // Parsed, so the schema is sent as a JSON object rather than a quoted string.
            put("schema", Json.parseToJsonElement(schema))
        }
    }

    val response = post("https://generativelanguage.googleapis.com/v1beta/interactions") {
        header("x-goog-api-key", System.getenv("GEMINI_API_KEY") ?: error("Set GEMINI_API_KEY to run the example"))
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

/**
 * Throws if left side argument doesn't contain the right side.
 */
infix fun String.shouldContain(string: String) {
    if (string !in this) {
        throw AssertionError("$string is not in $this")
    }
}

/**
 * Throws if caller is not null.
 */
fun <T> T?.shouldBeNull() {
    if (this != null) {
        throw AssertionError("$this is not null")
    }
}

/**
 * Throws if left side argument is not equal to the right side.
 */
infix fun <T> T.shouldBe(string: T) {
    if (this != string) {
        throw AssertionError("$this is not equal to $string")
    }
}
