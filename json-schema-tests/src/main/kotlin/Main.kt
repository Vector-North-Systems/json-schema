import com.vectornorthsystems.samples.Gemini
import com.vectornorthsystems.samples.Model
import com.vectornorthsystems.samples.classifyLibrary
import com.vectornorthsystems.samples.extractPublication
import com.vectornorthsystems.samples.extractRecipe

/**
 * The samples, by the name that selects them on the command line.
 */
val samples: Map<String, suspend (Model) -> Unit> = mapOf(
    "library" to ::classifyLibrary,
    "publication" to ::extractPublication,
    "recipe" to ::extractRecipe,
)

/**
 * Runs the samples named in [args], or all of them, with the model chosen by the MODEL environment variable.
 *
 * Run them all with Gemini:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run
 *
 * Or only some of them:
 *   GEMINI_API_KEY=... ./gradlew :json-schema-tests:run --args="recipe publication"
 */
suspend fun main(args: Array<String>) {
    val unknown = args.filterNot { it in samples }
    require(unknown.isEmpty()) { "Unknown samples: $unknown. Choose from: ${samples.keys}" }
    val selected = if (args.isEmpty()) samples else samples.filterKeys { it in args }

    // Use the model to run the selected samples.
    model().use { model ->
        selected.forEach { (name, sample) ->
            println("== $name")
            sample(model)
        }
    }
}

/**
 * The model named by the MODEL environment variable, Gemini by default.
 */
private fun model(): Model = when (val name = System.getenv("MODEL") ?: "gemini") {
    "gemini" -> Gemini(apiKey = System.getenv("GEMINI_API_KEY") ?: error("Set GEMINI_API_KEY to run the samples"))
    else -> error("Unknown model: $name. Choose from: [gemini]")
}
