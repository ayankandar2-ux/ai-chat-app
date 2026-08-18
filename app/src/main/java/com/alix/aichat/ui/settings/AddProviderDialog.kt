package com.alix.aichat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.alix.aichat.data.provider.ApiFormat
import com.alix.aichat.data.provider.ProviderConfig
import com.alix.aichat.data.provider.ProviderTemplate
import com.alix.aichat.data.provider.ProviderTemplates
import java.util.UUID

private enum class AddProviderStep { PICK_TEMPLATE, ENTER_DETAILS }

/**
 * Two-step "add provider" flow:
 *  1. Pick a template (Gemini, Groq, OpenRouter, ... or Custom)
 *  2. Confirm/edit the display name, base URL (editable for Custom), model,
 *     and paste an API key.
 *
 * Nothing is written to disk from here — [onConfirm] hands back a fully-formed
 * [ProviderConfig] and the caller (SettingsScreen -> MainActivity) is
 * responsible for appending it to the provider list and persisting it via
 * SecureStore, same as every other mutation in this app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderDialog(
    onDismiss: () -> Unit,
    onConfirm: (ProviderConfig) -> Unit
) {
    var step by remember { mutableStateOf(AddProviderStep.PICK_TEMPLATE) }
    var selectedTemplate by remember { mutableStateOf<ProviderTemplate?>(null) }

    var displayName by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (step == AddProviderStep.PICK_TEMPLATE) "Add provider"
                else selectedTemplate?.name ?: "Add provider"
            )
        },
        text = {
            when (step) {
                AddProviderStep.PICK_TEMPLATE -> TemplateList { template ->
                    selectedTemplate = template
                    displayName = template.name
                    baseUrl = template.baseUrl
                    model = template.defaultModel
                    apiKey = ""
                    step = AddProviderStep.ENTER_DETAILS
                }

                AddProviderStep.ENTER_DETAILS -> {
                    val template = selectedTemplate
                    val isCustom = template != null && template.baseUrl.isEmpty()

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isCustom) {
                            OutlinedTextField(
                                value = baseUrl,
                                onValueChange = { baseUrl = it },
                                label = { Text("Base URL") },
                                singleLine = true,
                                placeholder = { Text("https://api.example.com/v1") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API key") },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showKey = !showKey }) {
                                    Text(if (showKey) "Hide" else "Show")
                                }
                            },
                            placeholder = { Text(if (template?.freeTierAvailable == true) "Free tier available" else "") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (template != null && template.baseUrl == "http://localhost:11434/v1") {
                            Text(
                                "Ollama runs locally and usually doesn't need a key — leave this blank.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                AddProviderStep.PICK_TEMPLATE -> {}
                AddProviderStep.ENTER_DETAILS -> {
                    val template = selectedTemplate
                    val canSave = template != null &&
                        displayName.isNotBlank() &&
                        baseUrl.isNotBlank() &&
                        model.isNotBlank()
                    TextButton(
                        onClick = {
                            val t = template ?: return@TextButton
                            onConfirm(
                                ProviderConfig(
                                    id = UUID.randomUUID().toString(),
                                    displayName = displayName.trim(),
                                    format = t.format,
                                    baseUrl = baseUrl.trim(),
                                    apiKey = apiKey.trim(),
                                    model = model.trim(),
                                    enabled = true
                                )
                            )
                        },
                        enabled = canSave
                    ) {
                        Text("Save")
                    }
                }
            }
        },
        dismissButton = {
            when (step) {
                AddProviderStep.PICK_TEMPLATE -> TextButton(onClick = onDismiss) { Text("Cancel") }
                AddProviderStep.ENTER_DETAILS -> TextButton(onClick = { step = AddProviderStep.PICK_TEMPLATE }) { Text("Back") }
            }
        }
    )
}

@Composable
private fun TemplateList(onPick: (ProviderTemplate) -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(ProviderTemplates.all) { template ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(template) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(template.name)
                    Text(
                        formatSubtitle(template),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (template.freeTierAvailable) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Free tier available",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatSubtitle(template: ProviderTemplate): String = when (template.format) {
    ApiFormat.OPENAI_COMPATIBLE -> if (template.baseUrl.isEmpty()) "Custom OpenAI-compatible endpoint" else template.baseUrl
    ApiFormat.GEMINI -> "Google Gemini"
    ApiFormat.ANTHROPIC -> "Anthropic"
}
