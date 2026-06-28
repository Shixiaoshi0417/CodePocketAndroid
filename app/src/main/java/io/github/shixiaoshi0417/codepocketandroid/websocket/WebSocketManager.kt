package io.github.shixiaoshi0417.codepocketandroid.websocket

import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager {

    private val url = "ws://127.0.0.1:8765/ws"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isManualDisconnect = false
    private var isReconnecting = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) {
            return
        }
        isManualDisconnect = false
        startConnection()
    }

    fun disconnect() {
        isManualDisconnect = true
        isReconnecting = false
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendMessage(message: String) {
        val connected = _connectionState.value == ConnectionState.CONNECTED
        if (!connected) return
        webSocket?.send(message)
        val chatMessage = ChatMessage(
            content = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + chatMessage
    }

    private fun startConnection() {
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val chatMessage = ChatMessage(
                    content = text,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + chatMessage
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                this@WebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                this@WebSocketManager.webSocket = null
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect || isReconnecting) return
        isReconnecting = true
        scope.launch {
            delay(3000)
            isReconnecting = false
            if (!isManualDisconnect) {
                startConnection()
            }
        }
    }
}
