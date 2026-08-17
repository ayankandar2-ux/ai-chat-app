package com.alix.aichat.data.provider

import kotlinx.serialization.Serializable

/**
 * One saved provider entry the user has configured — a name, a request format,
 * an endpoint, a key, and which model to use. This is the only thing that gets
 * persisted per provider; everything else is derived at runtime.
 */
@Serializable
data class ProviderConfig(
    val id: String,               // stable local id, e.g. "gemini-main"
    val displayName: String,      // shown in the UI, e.g. "Gemini 2.5 Flash"
    val format: ApiFormat,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val enabled: Boolean = true
)

/**
 * The three request/response shapes that cover almost every hosted LLM API.
 * Adding a new provider is usually just a new ProviderTemplate, not new code.
 */
@Serializable
enum class ApiFormat {
    OPENAI_COMPATIBLE, // OpenAI, Groq, DeepSeek, Mistral, Together, OpenRouter,
                        // Perplexity, xAI/Grok, Fireworks, local Ollama, etc.
    GEMINI,             // Google Gemini
    ANTHROPIC           // Claude
}

data class ChatMessage(
    val role: Role,
    val content: String
)

enum class Role { USER, ASSISTANT, SYSTEM }

/**
 * Normalized outcome of a provider call. Every adapter maps its vendor's raw
 * HTTP response/error into one of these so the rest of the app never has to
 * know which vendor it's talking to.
 */
sealed class ProviderResult {
    data class Success(val message: ChatMessage) : ProviderResult()
    data class Failure(val error: ProviderError) : ProviderResult()
}

/**
 * Categorized failure reasons. This is what makes "self-healing" possible:
 * the repository layer decides what to do based on the *category*, not a raw
 * error string, and only INVALID_KEY / OUT_OF_QUOTA ever need to interrupt
 * the user — everything else is handled silently.
 */
sealed class ProviderError(val message: String) {
    class InvalidKey(message: String) : ProviderError(message)       // needs user action
    class OutOfQuota(message: String) : ProviderError(message)       // needs user action
    class RateLimited(val retryAfterSeconds: Int?, message: String) : ProviderError(message) // auto-retry
    class ModelNotFound(val requestedModel: String, message: String) : ProviderError(message) // auto-correct
    class Network(message: String) : ProviderError(message)          // auto-retry
    class ServerError(val httpCode: Int, message: String) : ProviderError(message) // auto-retry
    class Unknown(message: String) : ProviderError(message)          // surface as-is
}

/**
 * Implemented once per ApiFormat (see OpenAICompatibleProvider, GeminiProvider,
 * AnthropicProvider). A ProviderConfig + one of these adapters together make a
 * usable provider.
 */
interface AIProviderAdapter {
    suspend fun send(config: ProviderConfig, history: List<ChatMessage>): ProviderResult
    suspend fun listModels(config: ProviderConfig): List<String>
}
