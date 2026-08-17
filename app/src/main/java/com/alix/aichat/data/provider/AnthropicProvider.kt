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
private data class AnthropicRequest(
    val model: String,
    val max_tokens: Int = 1024,
    val messages: List<AnthropicMessage>
)

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class AnthropicResponse(val content: List<AnthropicBlock> = emptyList())

@Serializable
private data class AnthropicBlock(val text: String = "")

class AnthropicProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : AIProviderAdapter {

    override suspend fun send(config: ProviderConfig, history: List<ChatMessage>): ProviderResult {
        val body = AnthropicRequest(
            model = config.model,
            messages = history.filter { it.role != Role.SYSTEM }.map {
                AnthropicMessage(if (it.role == Role.USER) "user" else "assistant", it.content)
            }
        )
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/messages")
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(json.encodeToString(AnthropicRequest.serializer(), body)
                .toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when (resp.code) {
                    200 -> {
                        val parsed = json.decodeFromString(AnthropicResponse.serializer(), text)
                        val content = parsed.content.firstOrNull()?.text
                        if (!content.isNullOrEmpty()) {
                            ProviderResult.Success(ChatMessage(Role.ASSISTANT, content))
                        } else {
                            ProviderResult.Failure(ProviderError.Unknown("Empty response from Claude"))
                        }
                    }
                    401 -> ProviderResult.Failure(ProviderError.InvalidKey("Claude: invalid API key"))
                    404 -> ProviderResult.Failure(ProviderError.ModelNotFound(config.model, "Claude: model '${config.model}' not found"))
                    429 -> ProviderResult.Failure(ProviderError.RateLimited(null, "Claude: rate limited"))
                    402 -> ProviderResult.Failure(ProviderError.OutOfQuota("Claude: out of credits"))
                    in 500..599 -> ProviderResult.Failure(ProviderError.ServerError(resp.code, "Claude: server error ${resp.code}"))
                    else -> ProviderResult.Failure(ProviderError.Unknown("Claude: HTTP ${resp.code}"))
                }
            }
        } catch (e: IOException) {
            ProviderResult.Failure(ProviderError.Network("Claude: ${e.message ?: "network error"}"))
        }
    }

    override suspend fun listModels(config: ProviderConfig): List<String> = emptyList()
}
