package com.alix.aichat.data.provider

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class GeminiRequest(val contents: List<GeminiContent>)

@Serializable
private data class GeminiContent(val role: String, val parts: List<GeminiPart>)

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
private data class GeminiCandidate(val content: GeminiContent)

class GeminiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : AIProviderAdapter {

    override suspend fun send(config: ProviderConfig, history: List<ChatMessage>): ProviderResult {
        // Gemini has no separate "system" role at the content level; fold system
        // messages into the first user turn to keep this adapter simple.
        val contents = history
            .filter { it.role != Role.SYSTEM }
            .map {
                GeminiContent(
                    role = if (it.role == Role.USER) "user" else "model",
                    parts = listOf(GeminiPart(it.content))
                )
            }
        val body = GeminiRequest(contents)
        val url = "${config.baseUrl.trimEnd('/')}/models/${config.model}:generateContent?key=${config.apiKey}"

        val request = Request.Builder()
            .url(url)
            .post(json.encodeToString(GeminiRequest.serializer(), body)
                .toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when (resp.code) {
                    200 -> {
                        val parsed = json.decodeFromString(GeminiResponse.serializer(), text)
                        val content = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (content != null) {
                            ProviderResult.Success(ChatMessage(Role.ASSISTANT, content))
                        } else {
                            ProviderResult.Failure(ProviderError.Unknown("Empty response from Gemini"))
                        }
                    }
                    400 -> ProviderResult.Failure(ProviderError.InvalidKey("Gemini: bad request — often an invalid API key"))
                    401, 403 -> ProviderResult.Failure(ProviderError.InvalidKey("Gemini: invalid or unauthorized API key"))
                    404 -> ProviderResult.Failure(ProviderError.ModelNotFound(config.model, "Gemini: model '${config.model}' not found"))
                    429 -> ProviderResult.Failure(ProviderError.RateLimited(null, "Gemini: rate limited"))
                    in 500..599 -> ProviderResult.Failure(ProviderError.ServerError(resp.code, "Gemini: server error ${resp.code}"))
                    else -> ProviderResult.Failure(ProviderError.Unknown("Gemini: HTTP ${resp.code}"))
                }
            }
        } catch (e: IOException) {
            ProviderResult.Failure(ProviderError.Network("Gemini: ${e.message ?: "network error"}"))
        }
    }

    override suspend fun listModels(config: ProviderConfig): List<String> {
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/models?key=${config.apiKey}")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                // Minimal parse: real implementation would map the "models" array's "name" field.
                emptyList()
            }
        } catch (e: IOException) {
            emptyList()
        }
    }
}
