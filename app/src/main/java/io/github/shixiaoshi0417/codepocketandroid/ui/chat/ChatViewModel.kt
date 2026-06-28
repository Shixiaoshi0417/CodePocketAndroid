package io.github.shixiaoshi0417.codepocketandroid.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.shixiaoshi0417.codepocketandroid.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

class ChatViewModel : ViewModel() {

    private val webSocketManager = WebSocketManager()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatViewModel()
            }
        }
    }

    init {
        webSocketManager.connect()
        observeIncomingMessages()
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            webSocketManager.incomingMessages.collect { message ->
                _messages.value = _messages.value + ChatMessage(text = message, isFromUser = false)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.value = _messages.value + ChatMessage(text = text, isFromUser = true)
        viewModelScope.launch {
            webSocketManager.sendMessage(text)
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
