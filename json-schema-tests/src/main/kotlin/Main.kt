import com.vectornorthsystems.samples.providers.Gemini
import com.vectornorthsystems.samples.providers.Model
import com.vectornorthsystems.samples.providers.OpenAi
import com.vectornorthsystems.samples.classifyLibrary
import com.vectornorthsystems.samples.extractCatalog
import com.vectornorthsystems.samples.extractCategory
import com.vectornorthsystems.samples.extractConference
import com.vectornorthsystems.samples.extractDescribedCategory
import com.vectornorthsystems.samples.extractFeaturingCategory
import com.vectornorthsystems.samples.extractListedCategory
import com.vectornorthsystems.samples.extractOptionalPromotingCategory
import com.vectornorthsystems.samples.extractPromotingCategory
import com.vectornorthsystems.samples.extractPublication
import com.vectornorthsystems.samples.extractRecipe
import com.vectornorthsystems.samples.extractScoreboard
import com.vectornorthsystems.samples.extractShipment
import com.vectornorthsystems.samples.extractStockItem
import kotlin.system.exitProcess

/**
 * The samples, by the name that selects them on the command line.
 */
val samples: Map<String, suspend (Model) -> Unit> = mapOf(
    "library" to ::classifyLibrary,
    "publication" to ::extractPublication,
    "recipe" to ::extractRecipe,
    "shipment" to ::extractShipment,
    "conference" to ::extractConference,
    "stock" to ::extractStockItem,
    "scoreboard" to ::extractScoreboard,
    "category" to ::extractCategory,
    "category-nullable" to ::extractPromotingCategory,
    "category-described" to ::extractDescribedCategory,
    "category-optional" to ::extractOptionalPromotingCategory,
    "category-defs" to ::extractCatalog,
    "category-through-list" to ::extractListedCategory,
    "category-through-optional" to ::extractFeaturingCategory,
)

/**
 * Runs the samples named in [args], or all of them, with the model chosen by the MODEL environment variable.
 *
 * Run them all with Gemini:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run
 *
 * Or only some of them:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="recipe publication library"
 *
 * Or with OpenAI:
 *   MODEL=openai OPENAI_API_KEY=... ./gradlew :json-schema-tests:run
 *
 * A sample that fails doesn't stop the others: a provider may reject one schema and accept the rest. The failures are
 * listed at the end, and the exit code is 1 if there are any.
 */
suspend fun main(args: Array<String>) {
    val unknown = args.filterNot { it in samples }
    require(unknown.isEmpty()) { "Unknown samples: $unknown. Choose from: ${samples.keys}" }
    val selected = if (args.isEmpty()) samples else samples.filterKeys { it in args }

    // Use the model to run the selected samples, keeping the ones that fail.
    val failures = mutableMapOf<String, Throwable>()
    model().use { model ->
        selected.forEach { (name, sample) ->
            println("== $name")
            try {
                sample(model)
            } catch (e: Exception) {
                // A rejected schema, or an answer that doesn't decode.
                failures[name] = e
                println("FAILED: $e")
            } catch (e: AssertionError) {
                // An answer that decodes, but isn't what the sample expects.
                failures[name] = e
                println("FAILED: $e")
            }
        }
    }

    println("== ${selected.size - failures.size} of ${selected.size} samples passed")
    if (failures.isNotEmpty()) {
        failures.forEach { (name, failure) -> println("$name: $failure") }
        exitProcess(1)
    }
}

/**
 * The model named by the MODEL environment variable, Gemini by default.
 */
private fun model(): Model = when (val name = System.getenv("MODEL") ?: "gemini") {
    "gemini" -> Gemini(apiKey = System.getenv("GEMINI_API_KEY") ?: error("Set GEMINI_API_KEY to run the samples"))
    "openai" -> OpenAi(apiKey = System.getenv("OPENAI_API_KEY") ?: error("Set OPENAI_API_KEY to run the samples"))
    else -> error("Unknown model: $name. Choose from: [gemini, openai]")
}
