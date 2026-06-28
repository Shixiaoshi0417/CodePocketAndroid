package io.github.shixiaoshi0417.codepocketandroid.viewmodel

import androidx.lifecycle.ViewModel
import io.github.shixiaoshi0417.codepocketandroid.model.AgentStatusType
import io.github.shixiaoshi0417.codepocketandroid.model.AgentStep
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.websocket.WebSocketManager
import kotlinx.coroutines.flow.StateFlow

class AgentViewModel(
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = webSocketManager.messages
    val agentSteps: StateFlow<List<AgentStep>> = webSocketManager.agentSteps

    val isProcessing: Boolean
        get() {
            val steps = agentSteps.value
            if (steps.isEmpty()) return false
            val lastType = steps.last().type
            return lastType != AgentStatusType.RESULT && lastType != AgentStatusType.ERROR
        }

    fun sendPrompt(prompt: String) {
        webSocketManager.sendAgent(prompt)
    }
}
