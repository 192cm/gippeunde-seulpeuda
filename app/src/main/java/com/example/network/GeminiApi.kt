package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Google Gemini text-generation API.
 * Docs: https://ai.google.dev/api/generate-content
 */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GeminiRequest,
    ): GeminiResponse
}

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float = 1.0f,
    val maxOutputTokens: Int = 256,
    // Disable 2.5-flash "thinking" so short taglines return directly and fast.
    val thinkingConfig: GeminiThinkingConfig? = GeminiThinkingConfig(0),
)

@JsonClass(generateAdapter = true)
data class GeminiThinkingConfig(
    @Json(name = "thinkingBudget") val thinkingBudget: Int,
)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent? = null)

/**
 * Thin wrapper over the Gemini endpoint.
 *
 * The key is injected at build time by the Secrets Gradle Plugin from `.env`.
 * If the key is missing or still the placeholder, [generate] returns null so
 * callers transparently fall back to their built-in copy — no crash, no empty UI.
 */
object GeminiRepository {

    private const val MODEL = "gemini-2.5-flash"
    private const val PLACEHOLDER = "MY_GEMINI_API_KEY"

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != PLACEHOLDER

    suspend fun generate(prompt: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(),
            )
            val response = ApiProvider.geminiApi.generateContent(MODEL, apiKey, request)
            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
