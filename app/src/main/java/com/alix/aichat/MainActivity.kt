package com.alix.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import com.alix.aichat.data.mcp.McpServerConfig
import com.alix.aichat.data.provider.ProviderConfig
import com.alix.aichat.data.provider.ProviderRepository
import com.alix.aichat.data.store.SecureStore
import com.alix.aichat.ui.chat.ChatScreen
import com.alix.aichat.ui.chat.ChatViewModel
import com.alix.aichat.ui.settings.SettingsScreen
import com.alix.aichat.ui.theme.AiChatTheme

private enum class Screen { CHAT, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = SecureStore(applicationContext)

        setContent {
            AiChatTheme {
                var screen by remember { mutableStateOf(Screen.CHAT) }
                var providers by remember { mutableStateOf(store.loadProviders()) }
                var mcpServers by remember { mutableStateOf(store.loadMcpServers()) }

                val repository = remember { ProviderRepository(providers) }
                val viewModel = remember { ChatViewModel(repository) }
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(providers) {
                    repository.updateProviders(providers)
                }

                Box {
                    when (screen) {
                        Screen.CHAT -> ChatScreen(
                            state = uiState,
                            onSend = viewModel::sendMessage,
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onDismissError = viewModel::dismissError
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            providers = providers,
                            mcpServers = mcpServers,
                            onBack = { screen = Screen.CHAT },
                            onAddProvider = { newProvider ->
                                providers = providers + newProvider
                                store.saveProviders(providers)
                            },
                            onToggleProvider = { provider, enabled ->
                                providers = providers.map {
                                    if (it.id == provider.id) it.copy(enabled = enabled) else it
                                }
                                store.saveProviders(providers)
                            },
                            onAddMcpServer = { newServer ->
                                mcpServers = mcpServers + newServer
                                store.saveMcpServers(mcpServers)
                            },
                            onToggleMcpServer = { server, enabled ->
                                mcpServers = mcpServers.map {
                                    if (it.id == server.id) it.copy(enabled = enabled) else it
                                }
                                store.saveMcpServers(mcpServers)
                            }
                        )
                    }
                }
            }
        }
    }
}
