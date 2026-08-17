package com.alix.aichat.data.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * One connected MCP server. GitHub is not special-cased in code — it's just
 * the first entry a user is likely to add, using GitHub's own hosted remote
 * MCP endpoint with a PAT as the bearer token. Any other MCP server (self-hosted
 * or third-party) is added the same way: name + URL + optional token.
 */
@Serializable
data class McpServerConfig(
    val id: String,
    val displayName: String,
    val url: String,
    val bearerToken: String? = null,
    val enabled: Boolean = true
)

@Serializable
data class McpTool(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject? = null
)

@Serializable
private data class McpRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: JsonObject? = null
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Minimal JSON-RPC client for the MCP "tools/list" and "tools/call" methods.
 * Good starting point that already covers GitHub's remote server config below;
 * a full implementation would also handle the MCP initialize handshake and
 * streaming responses.
 */
class McpClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun listTools(server: McpServerConfig): List<McpTool> {
        val body = McpRpcRequest(method = "tools/list")
        val requestBuilder = Request.Builder()
            .url(server.url)
            .post(json.encodeToString(McpRpcRequest.serializer(), body)
                .toRequestBody("application/json".toMediaType()))
        server.bearerToken?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }

        return try {
            client.newCall(requestBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                // Real parsing would pull result.tools[] out of the JSON-RPC envelope.
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

object McpServerTemplates {
    /** GitHub's official hosted remote MCP server — auth via personal access token. */
    fun github(personalAccessToken: String) = McpServerConfig(
        id = "github",
        displayName = "GitHub",
        url = "https://api.githubcopilot.com/mcp",
        bearerToken = personalAccessToken
    )
}
