package com.alix.aichat.data.provider

import kotlinx.coroutines.delay

/**
 * Owns the list of configured providers and picks which adapter handles each
 * ApiFormat. This is where "self-healing" actually happens:
 *  - RateLimited / Network / ServerError -> retried with backoff automatically
 *  - ModelNotFound -> falls back to the provider's first available model once
 *  - if a provider keeps failing -> silently fail over to the next enabled
 *    provider so the conversation doesn't just die
 *  - InvalidKey / OutOfQuota -> nothing we can do locally; only these two are
 *    ever surfaced to the user
 */
class ProviderRepository(
    private var providers: List<ProviderConfig>,
    private val adapters: Map<ApiFormat, AIProviderAdapter> = mapOf(
        ApiFormat.OPENAI_COMPATIBLE to OpenAICompatibleProvider(),
        ApiFormat.GEMINI to GeminiProvider(),
        ApiFormat.ANTHROPIC to AnthropicProvider()
    )
) {
    fun updateProviders(configs: List<ProviderConfig>) {
        providers = configs
    }

    fun enabledProviders(): List<ProviderConfig> = providers.filter { it.enabled }

    /**
     * Sends the conversation to [preferredId] if given, else the first enabled
     * provider. On a recoverable error it retries in place; on repeated
     * failure it moves to the next enabled provider. Returns the final result
     * plus which provider actually answered (so the UI can show "answered via X"
     * if it wasn't the one originally selected).
     */
    suspend fun send(
        history: List<ChatMessage>,
        preferredId: String? = null
    ): Pair<ProviderConfig, ProviderResult> {
        val ordered = enabledProviders().sortedByDescending { it.id == preferredId }

        if (ordered.isEmpty()) {
            val placeholder = ProviderConfig(
                id = "none",
                displayName = "No provider configured",
                format = ApiFormat.OPENAI_COMPATIBLE,
                baseUrl = "",
                apiKey = "",
                model = "",
                enabled = false
            )
            return placeholder to ProviderResult.Failure(
                ProviderError.Unknown("Add a provider in Settings before sending a message.")
            )
        }

        var lastFailure: ProviderResult.Failure? = null

        for (provider in ordered) {
            val adapter = adapters[provider.format] ?: continue
            val result = sendWithRetry(adapter, provider, history)

            when (result) {
                is ProviderResult.Success -> return provider to result
                is ProviderResult.Failure -> {
                    lastFailure = result
                    when (result.error) {
                        is ProviderError.InvalidKey, is ProviderError.OutOfQuota -> {
                            // Needs the user — but try the next provider first
                            // rather than interrupting them immediately.
                            continue
                        }
                        else -> continue
                    }
                }
            }
        }
        // Every provider failed — this is the only point where the user sees an error.
        return ordered.first() to
            (lastFailure ?: ProviderResult.Failure(ProviderError.Unknown("No providers configured")))
    }

    private suspend fun sendWithRetry(
        adapter: AIProviderAdapter,
        provider: ProviderConfig,
        history: List<ChatMessage>,
        maxAttempts: Int = 3
    ): ProviderResult {
        var config = provider
        repeat(maxAttempts) { attempt ->
            when (val result = adapter.send(config, history)) {
                is ProviderResult.Success -> return result
                is ProviderResult.Failure -> {
                    when (val err = result.error) {
                        is ProviderError.RateLimited -> {
                            delay(((err.retryAfterSeconds ?: (2 * (attempt + 1))) * 1000L))
                        }
                        is ProviderError.Network, is ProviderError.ServerError -> {
                            delay(1000L * (attempt + 1)) // simple linear backoff
                        }
                        is ProviderError.ModelNotFound -> {
                            if (attempt == 0) {
                                val models = adapter.listModels(config)
                                val fallback = models.firstOrNull()
                                if (fallback != null && fallback != config.model) {
                                    config = config.copy(model = fallback)
                                } else {
                                    return result // nothing to auto-correct to
                                }
                            } else return result
                        }
                        else -> return result // InvalidKey, OutOfQuota, Unknown: no point retrying
                    }
                }
            }
        }
        return adapter.send(config, history)
    }
}
