package io.github.shixiaoshi0417.codepocketandroid.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager {

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _incomingMessages = MutableSharedFlow<String>(replay = 0)
    val incomingMessages: SharedFlow<String> = _incomingMessages

    fun connect(url: String = "ws://127.0.0.1:8765/ws") {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                CoroutineScope(Dispatchers.IO).launch {
                    _incomingMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocket failure: ${t.message}")
            }
        })
    }

    fun sendMessage(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closing")
        webSocket = null
    }
}
