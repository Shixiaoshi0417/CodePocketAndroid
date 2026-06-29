package io.github.shixiaoshi0417.codepocketandroid.viewmodel

import androidx.lifecycle.ViewModel
import io.github.shixiaoshi0417.codepocketandroid.model.AgentStep
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.websocket.WebSocketManager
import kotlinx.coroutines.flow.StateFlow

class AgentViewModel(
    private val webSocketManager: WebSocketManager,
    isProcessingFlow: StateFlow<Boolean>
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = webSocketManager.messages
    val isProcessing: StateFlow<Boolean> = isProcessingFlow

    fun sendPrompt(prompt: String, model: String = "", sessionId: String = "") {
        webSocketManager.sendAgent(prompt, model.ifEmpty { "deepseek-v4-pro" }, sessionId)
    }
}
