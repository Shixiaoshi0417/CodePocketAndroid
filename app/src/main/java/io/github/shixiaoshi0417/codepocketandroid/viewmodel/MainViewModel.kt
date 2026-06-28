package io.github.shixiaoshi0417.codepocketandroid.viewmodel

import androidx.lifecycle.ViewModel
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.websocket.WebSocketManager
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val webSocketManager = WebSocketManager()

    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    val messages: StateFlow<List<ChatMessage>> = webSocketManager.messages

    fun connect() {
        webSocketManager.connect()
    }

    fun disconnect() {
        webSocketManager.disconnect()
    }

    fun sendMessage(message: String) {
        webSocketManager.sendMessage(message)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
