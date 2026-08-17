package com.alix.aichat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alix.aichat.data.provider.ChatMessage
import com.alix.aichat.data.provider.ProviderRepository
import com.alix.aichat.data.provider.ProviderResult
import com.alix.aichat.data.provider.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorText: String? = null,
    val activeProviderName: String? = null
)

class ChatViewModel(private val repository: ProviderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMessage = ChatMessage(Role.USER, text)
        _uiState.update { it.copy(messages = it.messages + userMessage, isSending = true, errorText = null) }

        viewModelScope.launch {
            val (provider, result) = repository.send(_uiState.value.messages)
            when (result) {
                is ProviderResult.Success -> _uiState.update {
                    it.copy(
                        messages = it.messages + result.message,
                        isSending = false,
                        activeProviderName = provider.displayName
                    )
                }
                is ProviderResult.Failure -> _uiState.update {
                    it.copy(isSending = false, errorText = result.error.message)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorText = null) }
    }
}
