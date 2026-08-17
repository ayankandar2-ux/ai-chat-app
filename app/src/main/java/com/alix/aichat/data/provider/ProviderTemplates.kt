package com.alix.aichat.data.provider

/**
 * A template is NOT a saved provider — it's a preset the settings screen offers
 * so the user just picks a name and pastes a key, instead of typing a base URL
 * and format by hand. "Custom" covers anything OpenAI-compatible that isn't
 * listed, which is most self-hosted / niche providers.
 */
data class ProviderTemplate(
    val name: String,
    val format: ApiFormat,
    val baseUrl: String,
    val defaultModel: String,
    val freeTierAvailable: Boolean
)

object ProviderTemplates {
    val all = listOf(
        ProviderTemplate("Gemini", ApiFormat.GEMINI, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", freeTierAvailable = true),
        ProviderTemplate("Groq", ApiFormat.OPENAI_COMPATIBLE, "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", freeTierAvailable = true),
        ProviderTemplate("OpenRouter", ApiFormat.OPENAI_COMPATIBLE, "https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free", freeTierAvailable = true),
        ProviderTemplate("DeepSeek", ApiFormat.OPENAI_COMPATIBLE, "https://api.deepseek.com/v1", "deepseek-chat", freeTierAvailable = true),
        ProviderTemplate("Mistral", ApiFormat.OPENAI_COMPATIBLE, "https://api.mistral.ai/v1", "mistral-small-latest", freeTierAvailable = true),
        ProviderTemplate("Together AI", ApiFormat.OPENAI_COMPATIBLE, "https://api.together.xyz/v1", "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free", freeTierAvailable = true),
        ProviderTemplate("Cerebras", ApiFormat.OPENAI_COMPATIBLE, "https://api.cerebras.ai/v1", "llama3.3-70b", freeTierAvailable = true),
        ProviderTemplate("xAI (Grok)", ApiFormat.OPENAI_COMPATIBLE, "https://api.x.ai/v1", "grok-4-fast", freeTierAvailable = false),
        ProviderTemplate("OpenAI", ApiFormat.OPENAI_COMPATIBLE, "https://api.openai.com/v1", "gpt-4o-mini", freeTierAvailable = false),
        ProviderTemplate("Claude (Anthropic)", ApiFormat.ANTHROPIC, "https://api.anthropic.com/v1", "claude-sonnet-4-6", freeTierAvailable = false),
        ProviderTemplate("Ollama (local)", ApiFormat.OPENAI_COMPATIBLE, "http://localhost:11434/v1", "llama3.2", freeTierAvailable = true),
        ProviderTemplate("Custom (OpenAI-compatible)", ApiFormat.OPENAI_COMPATIBLE, "", "", freeTierAvailable = true)
    )
}
