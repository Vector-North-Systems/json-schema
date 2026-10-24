import com.vectornorthsystems.samples.classifyLibrary
import com.vectornorthsystems.samples.extractPublication

/**
 * Runs the library samples.
 * Each sample may require specific env vars and/or API keys, see the corresponding source code for details.
 *
 *  * Run it with a Gemini API key:
 *  *     GEMINI_API_KEY=... ./gradlew :json-schema-tests:run
 */
suspend fun main() {
    classifyLibrary()
    extractPublication()
}
