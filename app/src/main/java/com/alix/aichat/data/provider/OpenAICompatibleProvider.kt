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
private data class OAChatRequest(
    val model: String,
    val messages: List<OAChatMessage>
)

@Serializable
private data class OAChatMessage(val role: String, val content: String)

@Serializable
private data class OAChatResponse(val choices: List<OAChoice> = emptyList())

@Serializable
private data class OAChoice(val message: OAChatMessage)

@Serializable
private data class OAModelsResponse(val data: List<OAModel> = emptyList())

@Serializable
private data class OAModel(val id: String)

/**
 * Handles any provider that speaks the OpenAI /v1/chat/completions shape.
 * This single adapter is what lets "add any API key" work for a large chunk
 * of the ecosystem without writing per-vendor code.
 */
class OpenAICompatibleProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : AIProviderAdapter {

    override suspend fun send(config: ProviderConfig, history: List<ChatMessage>): ProviderResult {
        val body = OAChatRequest(
            model = config.model,
            messages = history.map {
                OAChatMessage(
                    role = when (it.role) {
                        Role.USER -> "user"
                        Role.ASSISTANT -> "assistant"
                        Role.SYSTEM -> "system"
                    },
                    content = it.content
                )
            }
        )
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(json.encodeToString(OAChatRequest.serializer(), body)
                .toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when (resp.code) {
                    200 -> {
                        val parsed = json.decodeFromString(OAChatResponse.serializer(), text)
                        val content = parsed.choices.firstOrNull()?.message?.content
                        if (content != null) {
                            ProviderResult.Success(ChatMessage(Role.ASSISTANT, content))
                        } else {
                            ProviderResult.Failure(ProviderError.Unknown("Empty response from ${config.displayName}"))
                        }
                    }
                    401, 403 -> ProviderResult.Failure(ProviderError.InvalidKey("${config.displayName}: invalid or unauthorized API key"))
                    404 -> ProviderResult.Failure(ProviderError.ModelNotFound(config.model, "${config.displayName}: model '${config.model}' not found"))
                    429 -> ProviderResult.Failure(ProviderError.RateLimited(parseRetryAfter(resp.header("Retry-After")), "${config.displayName}: rate limited"))
                    402 -> ProviderResult.Failure(ProviderError.OutOfQuota("${config.displayName}: out of quota/credits"))
                    in 500..599 -> ProviderResult.Failure(ProviderError.ServerError(resp.code, "${config.displayName}: server error ${resp.code}"))
                    else -> ProviderResult.Failure(ProviderError.Unknown("${config.displayName}: HTTP ${resp.code}"))
                }
            }
        } catch (e: IOException) {
            ProviderResult.Failure(ProviderError.Network("${config.displayName}: ${e.message ?: "network error"}"))
        }
    }

    override suspend fun listModels(config: ProviderConfig): List<String> {
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/models")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val text = resp.body?.string().orEmpty()
                json.decodeFromString(OAModelsResponse.serializer(), text).data.map { it.id }
            }
        } catch (e: IOException) {
            emptyList()
        }
    }

    private fun parseRetryAfter(header: String?): Int? = header?.toIntOrNull()
}
