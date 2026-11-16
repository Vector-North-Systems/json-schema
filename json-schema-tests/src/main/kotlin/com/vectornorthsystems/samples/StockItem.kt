package com.vectornorthsystems.samples

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.samples.providers.Model
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A value class, described by the type it wraps: a string.
 */
@Serializable
@JvmInline
value class Sku(val value: String)

/**
 * A generic class, described once per type argument: [StockItem] uses it with a `Double` and with an `Int`.
 */
@Serializable
data class Measured<T>(
    val value: T,
    val unit: String,
)

@Serializable
data class Dimensions(
    val widthCm: Double,
    val heightCm: Double,
)

@Serializable
@SchemaRoot
data class StockItem(
    // A value class: "type": "string".
    val sku: Sku,

    // A Char: "type": "string".
    @SchemaDescription("The letter of the aisle where the item is stored")
    val aisle: Char,

    // An unsigned type: "type": "integer" with "minimum": 0.
    val unitsInStock: UInt,

    // A generic class with a number, and with an integer.
    val weight: Measured<Double>,
    val pack: Measured<Int>,

    // A nullable class used once, so it's written in place: "type": ["object", "null"].
    val dimensions: Dimensions?,

    // A nullable array: "type": ["array", "null"].
    val barcodes: List<String>?,

    // A primitive array: an array of integers.
    @SchemaDescription("The units sold in each of the last four weeks, oldest first")
    val weeklySales: IntArray,

    // An array of arrays.
    @SchemaDescription("The shelf positions that hold the item, one inner list per row")
    val shelfRows: List<List<Int>>,

    val discontinued: Boolean,
)

/**
 * Checks the types the other samples don't send to a provider: a value class, a `Char`, an unsigned integer, a
 * generic class, a primitive array, an array of arrays, and a nullable object and a nullable array written in place.
 *
 * Run it with a Gemini API key:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="stock"
 */
suspend fun extractStockItem(model: Model) {
    println("The StockItem class JSON schema is: ${StockItemSchema[model.target]}")

    val input = """
        Extract the stock item described below. Answer null for what the text doesn't give.

        Item KB-2041 is stored in aisle C, and there are 12 units in stock. It weighs 0.85 kg and is sold in packs of
        6 pieces. Its barcodes are 4006381333931 and 5901234123457. The text doesn't give its dimensions.
        It sold 3, 0, 7 and 5 units in the last four weeks, in that order.
        It's on two shelf rows: positions 1, 2 and 3 on the first, and positions 4 and 5 on the second.
        It hasn't been discontinued.
    """.trimIndent()

    val modelResponse = model.generate(input, StockItemSchema[model.target])
    println("Model response: $modelResponse")

    // The default Json is strict: an aisle longer than one character, or a negative stock, fails here.
    val item = Json.decodeFromString<StockItem>(modelResponse)

    item.sku shouldBe Sku("KB-2041")
    item.aisle shouldBe 'C'
    item.unitsInStock shouldBe 12u

    item.weight.value shouldBe 0.85
    item.pack.value shouldBe 6

    // The nullable object takes an explicit null, and the nullable array takes its items.
    item.dimensions.shouldBeNull()
    item.barcodes shouldBe listOf("4006381333931", "5901234123457")

    item.weeklySales.toList() shouldBe listOf(3, 0, 7, 5)
    item.shelfRows shouldBe listOf(listOf(1, 2, 3), listOf(4, 5))
    item.discontinued shouldBe false
}
