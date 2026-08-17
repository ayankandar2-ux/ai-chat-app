package com.alix.aichat.data.store

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.alix.aichat.data.mcp.McpServerConfig
import com.alix.aichat.data.provider.ApiFormat
import com.alix.aichat.data.provider.ProviderConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * No login system: everything (API keys, MCP tokens, chat prefs) lives in
 * EncryptedSharedPreferences on-device, tied to the Android Keystore. Nothing
 * leaves the phone except direct calls to the providers/MCP servers the user
 * configured themselves.
 */
class SecureStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ai_chat_secure_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun saveProviders(configs: List<ProviderConfig>) {
        prefs.edit().putString(KEY_PROVIDERS, json.encodeToString(SerializableList.serializer(), SerializableList(configs))).apply()
    }

    fun loadProviders(): List<ProviderConfig> {
        val raw = prefs.getString(KEY_PROVIDERS, null) ?: return emptyList()
        return runCatching { json.decodeFromString(SerializableList.serializer(), raw).items }.getOrDefault(emptyList())
    }

    fun saveMcpServers(configs: List<McpServerConfig>) {
        prefs.edit().putString(KEY_MCP, json.encodeToString(McpList.serializer(), McpList(configs))).apply()
    }

    fun loadMcpServers(): List<McpServerConfig> {
        val raw = prefs.getString(KEY_MCP, null) ?: return emptyList()
        return runCatching { json.decodeFromString(McpList.serializer(), raw).items }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_PROVIDERS = "providers"
        private const val KEY_MCP = "mcp_servers"
    }
}

@Serializable
private data class SerializableList(val items: List<ProviderConfig>)

@Serializable
private data class McpList(val items: List<McpServerConfig>)
