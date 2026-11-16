package com.vectornorthsystems.samples

import com.vectornorthsystems.jsonschema.SchemaDescription
import com.vectornorthsystems.jsonschema.SchemaRoot
import com.vectornorthsystems.jsonschema.SchemaTarget
import com.vectornorthsystems.samples.providers.Model
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Recursive classes, one step at a time, so a provider that rejects one of them shows which step it doesn't accept.
 *
 * Gemini rejects a required, nullable property that leads back to its own class, so the processor reports it as a
 * build error for the GEMINI dialect. The classes that have one are only generated for OpenAI, and their samples are
 * skipped with Gemini. See item 17 of docs/improvements.md.
 *
 * Run them all:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="category category-nullable category-described category-optional category-defs category-through-list category-through-optional"
 */

/**
 * A root that contains itself. References to the root are written as `"$ref": "#"`, without `$defs`.
 */
@Serializable
@SchemaRoot
data class Category(
    val name: String,

    // An array of references to the root: "items": {"$ref": "#"}.
    val subcategories: List<Category>,
)

/**
 * [Category], plus a nullable reference to the root. It's required, so Gemini rejects it.
 */
@Serializable
@SchemaRoot(targets = [SchemaTarget.OPENAI])
data class PromotingCategory(
    val name: String,
    val subcategories: List<PromotingCategory>,

    // A nullable reference to the root: "anyOf": [{"$ref": "#"}, {"type": "null"}].
    val promoted: PromotingCategory?,
)

/**
 * [PromotingCategory], plus a description on the nullable reference.
 */
@Serializable
@SchemaRoot(targets = [SchemaTarget.OPENAI])
data class DescribedCategory(
    val name: String,
    val subcategories: List<DescribedCategory>,

    // A description next to the anyOf of a nullable reference to the root.
    @SchemaDescription("The subcategory promoted on the category's page, or null if there isn't one")
    val promoted: DescribedCategory?,
)

/**
 * [PromotingCategory], with the nullable reference optional instead of required, which Gemini accepts. Only differs in
 * the dialects that have optional properties.
 */
@Serializable
@SchemaRoot
data class OptionalPromotingCategory(
    val name: String,
    val subcategories: List<OptionalPromotingCategory>,

    // The same nullable reference to the root, left out of "required".
    val promoted: OptionalPromotingCategory? = null,
)

/**
 * [PromotingCategory], one level down: the recursive class isn't the root, so it's written in `$defs`, and its
 * references are `"$ref": "#/$defs/CatalogCategory"` instead of `"$ref": "#"`. Gemini rejects it too.
 */
@Serializable
@SchemaRoot(targets = [SchemaTarget.OPENAI])
data class Catalog(
    val top: CatalogCategory,
)

@Serializable
data class CatalogCategory(
    val name: String,
    val subcategories: List<CatalogCategory>,

    // A nullable reference to a definition: "anyOf": [{"$ref": "#/$defs/CatalogCategory"}, {"type": "null"}].
    val promoted: CatalogCategory?,
)

/**
 * A required, nullable property that leads back to the root, where the way back goes through an array:
 * [ListedCategory.subcategories] is a list of [Subcategory], and [Subcategory.parent] is the root again.
 *
 * The processor accepts it for Gemini, on the assumption that an array, which can be empty, makes the difference.
 * This sample checks that assumption.
 */
@Serializable
@SchemaRoot
data class ListedCategory(
    val name: String,
    val subcategories: List<Subcategory>,
)

@Serializable
data class Subcategory(
    val name: String,

    // Required and nullable, back to the root: "anyOf": [{"$ref": "#"}, {"type": "null"}].
    @SchemaDescription("Always null")
    val parent: ListedCategory?,
)

/**
 * A required, nullable property that leads back to the root, where the way back goes through an optional property:
 * [FeaturingCategory.featured] is optional, and [Featured.category] is the root again.
 *
 * The processor accepts it for Gemini, on the assumption that an optional property anywhere in the cycle is enough.
 * This sample checks that assumption.
 */
@Serializable
@SchemaRoot
data class FeaturingCategory(
    val name: String,
    val subcategories: List<String>,

    // Optional.
    val featured: Featured? = null,
)

@Serializable
data class Featured(
    val reason: String,

    // Required and nullable, back to the root: "anyOf": [{"$ref": "#"}, {"type": "null"}].
    @SchemaDescription("Always null")
    val category: FeaturingCategory?,
)

private val categoryInput = """
    Extract the category tree described below. A category without subcategories has an empty list.

    Electronics
      Computers
        Laptops
        Desktops
      Phones

    No category has a promoted or a featured subcategory.
""".trimIndent()

/**
 * Sends [categoryInput] with [schema], or says the sample is skipped when the class isn't generated for the model's
 * dialect.
 *
 * @return The model's answer, or null when skipped.
 */
private suspend fun Model.extractCategory(className: String, schema: String?): String? {
    if (schema == null) {
        println("Skipped: $className has a required property that leads back to its own class, which the $target dialect doesn't support.")
        return null
    }
    println("The $className class JSON schema is: $schema")

    return generate(categoryInput, schema).also { println("Model response: $it") }
}

/**
 * Checks a recursive root with a provider. The publication sample has a recursive class in `$defs`; here the
 * references point at the root itself.
 */
suspend fun extractCategory(model: Model) {
    val modelResponse = model.extractCategory("Category", CategorySchema.getOrNull(model.target)) ?: return

    val electronics = Json.decodeFromString<Category>(modelResponse)

    // The root references itself, two levels deep.
    electronics.name shouldBe "Electronics"
    electronics.subcategories.map { it.name } shouldBe listOf("Computers", "Phones")
    val (computers, phones) = electronics.subcategories
    computers.subcategories.map { it.name } shouldBe listOf("Laptops", "Desktops")
    phones.subcategories shouldBe emptyList()
}

/**
 * Checks a nullable reference to the root, on top of what [extractCategory] checks.
 */
suspend fun extractPromotingCategory(model: Model) {
    val modelResponse = model.extractCategory("PromotingCategory", PromotingCategorySchema.getOrNull(model.target))
        ?: return

    val electronics = Json.decodeFromString<PromotingCategory>(modelResponse)

    electronics.name shouldBe "Electronics"
    electronics.subcategories.map { it.name } shouldBe listOf("Computers", "Phones")

    // The nullable reference to the root takes an explicit null, at every level.
    electronics.promoted.shouldBeNull()
    electronics.subcategories.first().promoted.shouldBeNull()
}

/**
 * Checks a description on a nullable reference to the root, on top of what [extractPromotingCategory] checks.
 */
suspend fun extractDescribedCategory(model: Model) {
    val modelResponse = model.extractCategory("DescribedCategory", DescribedCategorySchema.getOrNull(model.target))
        ?: return

    val electronics = Json.decodeFromString<DescribedCategory>(modelResponse)

    electronics.name shouldBe "Electronics"
    electronics.subcategories.map { it.name } shouldBe listOf("Computers", "Phones")
    electronics.promoted.shouldBeNull()
    electronics.subcategories.first().promoted.shouldBeNull()
}

/**
 * Checks [extractPromotingCategory]'s nullable reference to the root when it's optional.
 */
suspend fun extractOptionalPromotingCategory(model: Model) {
    val modelResponse = model.extractCategory(
        "OptionalPromotingCategory",
        OptionalPromotingCategorySchema.getOrNull(model.target)
    ) ?: return

    val electronics = Json.decodeFromString<OptionalPromotingCategory>(modelResponse)

    electronics.name shouldBe "Electronics"
    electronics.subcategories.map { it.name } shouldBe listOf("Computers", "Phones")
    // Left out or null, both decode to null.
    electronics.promoted.shouldBeNull()
}

/**
 * Checks [extractPromotingCategory]'s nullable recursive reference when it points at `$defs` instead of the root.
 */
suspend fun extractCatalog(model: Model) {
    val modelResponse = model.extractCategory("Catalog", CatalogSchema.getOrNull(model.target)) ?: return

    val electronics = Json.decodeFromString<Catalog>(modelResponse).top

    electronics.name shouldBe "Electronics"
    electronics.subcategories.map { it.name } shouldBe listOf("Computers", "Phones")
    electronics.promoted.shouldBeNull()
    electronics.subcategories.first().promoted.shouldBeNull()
}

/**
 * Checks a required, nullable property that leads back to the root through an array.
 */
suspend fun extractListedCategory(model: Model) {
    val modelResponse = model.extractCategory("ListedCategory", ListedCategorySchema.getOrNull(model.target))
        ?: return

    val electronics = Json.decodeFromString<ListedCategory>(modelResponse)

    electronics.name shouldBe "Electronics"
    electronics.subcategories.map { it.name } shouldBe listOf("Computers", "Phones")
    electronics.subcategories.forEach { it.parent.shouldBeNull() }
}

/**
 * Checks a required, nullable property that leads back to the root through an optional property.
 */
suspend fun extractFeaturingCategory(model: Model) {
    val modelResponse = model.extractCategory("FeaturingCategory", FeaturingCategorySchema.getOrNull(model.target))
        ?: return

    val electronics = Json.decodeFromString<FeaturingCategory>(modelResponse)

    electronics.name shouldBe "Electronics"
    electronics.subcategories shouldBe listOf("Computers", "Phones")
    // The sample checks that the provider accepts the schema, and that the answer decodes, which it did to get here.
    // What the model puts in `featured` isn't checked: the property is optional, not forbidden, and Gemini fills it
    // in, sometimes with a category nested inside, whatever the input says.
}
