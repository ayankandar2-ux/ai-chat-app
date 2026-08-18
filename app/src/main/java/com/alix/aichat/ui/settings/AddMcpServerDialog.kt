package com.alix.aichat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.alix.aichat.data.mcp.McpServerConfig
import java.util.UUID

/**
 * "Add MCP server": name, URL, optional bearer token. A "Use GitHub's hosted
 * server" shortcut fills in GitHub's official remote MCP endpoint since that's
 * the connector most people add first — everything else (self-hosted or any
 * other third-party MCP server) is entered the same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (McpServerConfig) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var bearerToken by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }

    val canSave = displayName.isNotBlank() && url.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MCP server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        displayName = "GitHub"
                        url = "https://api.githubcopilot.com/mcp"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use GitHub's hosted server")
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bearerToken,
                    onValueChange = { bearerToken = it },
                    label = { Text("Token (optional)") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showToken = !showToken }) {
                            Text(if (showToken) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        McpServerConfig(
                            id = UUID.randomUUID().toString(),
                            displayName = displayName.trim(),
                            url = url.trim(),
                            bearerToken = bearerToken.trim().ifBlank { null },
                            enabled = true
                        )
                    )
                },
                enabled = canSave
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
