package com.alix.aichat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alix.aichat.data.mcp.McpServerConfig
import com.alix.aichat.data.provider.ProviderConfig

@Composable
fun SettingsScreen(
    providers: List<ProviderConfig>,
    mcpServers: List<McpServerConfig>,
    onBack: () -> Unit,
    onAddProvider: () -> Unit,
    onToggleProvider: (ProviderConfig, Boolean) -> Unit,
    onAddMcpServer: () -> Unit,
    onToggleMcpServer: (McpServerConfig, Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            item { SectionHeader("AI Providers") }
            items(providers) { provider ->
                SettingsRow(
                    title = provider.displayName,
                    subtitle = provider.model,
                    checked = provider.enabled,
                    onCheckedChange = { onToggleProvider(provider, it) }
                )
            }
            item { AddRow("Add provider", onAddProvider) }

            item { Spacer(Modifier.height(24.dp)) }

            item { SectionHeader("MCP Connectors") }
            items(mcpServers) { server ->
                SettingsRow(
                    title = server.displayName,
                    subtitle = server.url,
                    checked = server.enabled,
                    onCheckedChange = { onToggleMcpServer(server, it) }
                )
            }
            item { AddRow("Add MCP server", onAddMcpServer) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun SettingsRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Add, contentDescription = label)
        }
        Text(label)
    }
}
