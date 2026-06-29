package io.github.shixiaoshi0417.codepocketandroid.websocket

import android.util.Log
import io.github.shixiaoshi0417.codepocketandroid.model.AgentRequest
import io.github.shixiaoshi0417.codepocketandroid.model.AgentStatusType
import io.github.shixiaoshi0417.codepocketandroid.model.AgentStep
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.model.ChatResponse
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.model.MessageRole
import io.github.shixiaoshi0417.codepocketandroid.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val onMessagePersist: (suspend (ChatMessage) -> Unit)? = null,
    private val onProcessingChange: ((Boolean) -> Unit)? = null
) {

    private val url = "ws://127.0.0.1:8765/ws"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isManualDisconnect = false
    private var isReconnecting = false

    var conversationId: String = ""

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val agentChannel = Channel<AgentEvent>(Channel.UNLIMITED)
    private var agentConsumerJob: Job? = null

    private val _agentSteps = MutableStateFlow<List<AgentStep>>(emptyList())
    val agentSteps: StateFlow<List<AgentStep>> = _agentSteps.asStateFlow()

    private fun startAgentConsumer() {
        agentConsumerJob?.cancel()
        agentConsumerJob = scope.launch(Dispatchers.Main) {
            for (event in agentChannel) {
                when (event) {
                    is AgentEvent.Start -> {
                        _agentSteps.value = _agentSteps.value + AgentStep(
                            type = AgentStatusType.STATUS,
                            content = "OpenCode Agent started"
                        )
                        val agentMsg = ChatMessage(
                            role = MessageRole.ASSISTANT,
                            content = "",
                            isStreaming = true,
                            conversationId = conversationId,
                            messageType = MessageType.AGENT_STATUS
                        )
                        _messages.value = _messages.value + agentMsg
                    }
                    is AgentEvent.Status -> {
                        _agentSteps.value = _agentSteps.value + AgentStep(
                            type = AgentStatusType.STATUS,
                            content = event.content
                        )
                        val current = _messages.value
                        if (current.isNotEmpty()) {
                            val last = current.last()
                            if (last.isStreaming && last.role == MessageRole.ASSISTANT) {
                                val updated = last.copy(
                                    content = last.content + event.content + "\n"
                                )
                                _messages.value = current.dropLast(1) + updated
                            }
                        }
                    }
                    is AgentEvent.Diff -> {
                        _agentSteps.value = _agentSteps.value + AgentStep(
                            type = AgentStatusType.DIFF,
                            content = event.content
                        )
                    }
                    is AgentEvent.Result -> {
                        _agentSteps.value = _agentSteps.value + AgentStep(
                            type = if (event.success) AgentStatusType.RESULT else AgentStatusType.ERROR,
                            content = event.content
                        )
                        onProcessingChange?.invoke(false)
                        val current = _messages.value
                        if (current.isNotEmpty()) {
                            val last = current.last()
                            if (last.isStreaming && last.role == MessageRole.ASSISTANT) {
                                val updated = last.copy(isStreaming = false, messageType = MessageType.AGENT_RESULT)
                                _messages.value = current.dropLast(1) + updated
                                persistMessage(updated)
                            }
                        }
                    }
                }
                delay(30)
            }
        }
    }

    init {
        startAgentConsumer()
    }

    fun switchConversation(convId: String, initialMessages: List<ChatMessage>) {
        conversationId = convId
        _messages.value = initialMessages
    }

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) return
        isManualDisconnect = false
        startConnection()
    }

    fun disconnect() {
        isManualDisconnect = true
        isReconnecting = false
        onProcessingChange?.invoke(false)
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun sendAgent(prompt: String, model: String = "", sessionId: String = "") {
        val connected = _connectionState.value == ConnectionState.CONNECTED
        if (!connected) { Log.e("WS_SEND", "AGENT blocked: not connected"); return }
        _agentSteps.value = emptyList()
        onProcessingChange?.invoke(true)
        val finalModel = model.ifEmpty { "deepseek-v4-pro" }
        val request = AgentRequest(type = "agent", prompt = prompt, model = finalModel, sessionId = sessionId)
        val requestBody = json.encodeToString(AgentRequest.serializer(), request)
        Log.e("WS_SEND", "AGENT: $requestBody")
        webSocket?.send(requestBody)
        val promptMsg = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            conversationId = conversationId,
            messageType = MessageType.CHAT
        )
        _messages.value = _messages.value + promptMsg
        persistMessage(promptMsg)
    }

    private fun persistMessage(message: ChatMessage) {
        onMessagePersist?.let { persist ->
            scope.launch { persist(message) }
        }
    }

    private fun startConnection() {
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val baseResponse = json.decodeFromString(ChatResponse.serializer(), text)
                    when (baseResponse.type) {
                        "agent_start" -> agentChannel.trySend(AgentEvent.Start)
                        "agent_status" -> agentChannel.trySend(AgentEvent.Status(baseResponse.content))
                        "agent_diff" -> agentChannel.trySend(AgentEvent.Diff(baseResponse.content))
                        "agent_result" -> agentChannel.trySend(
                            AgentEvent.Result(
                                content = baseResponse.content,
                                success = baseResponse.success ?: false
                            )
                        )
                        "error" -> {
                            val errorMsg = ChatMessage(
                                role = MessageRole.SYSTEM,
                                content = "Error: ${baseResponse.message.ifEmpty { "Unknown error" }}",
                                conversationId = conversationId,
                                messageType = MessageType.SYSTEM
                            )
                            scope.launch(Dispatchers.Main) {
                                _messages.value = _messages.value + errorMsg
                            }
                            persistMessage(errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = ChatMessage(
                        role = MessageRole.SYSTEM,
                        content = "Error: ${e.message}",
                        conversationId = conversationId,
                        messageType = MessageType.SYSTEM
                    )
                    scope.launch(Dispatchers.Main) {
                        _messages.value = _messages.value + errorMsg
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                this@WebSocketManager.webSocket = null
                onProcessingChange?.invoke(false)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                this@WebSocketManager.webSocket = null
                onProcessingChange?.invoke(false)
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
            if (!isManualDisconnect) startConnection()
        }
    }
}

sealed class AgentEvent {
    data object Start : AgentEvent()
    data class Status(val content: String) : AgentEvent()
    data class Diff(val content: String) : AgentEvent()
    data class Result(val content: String, val success: Boolean) : AgentEvent()
}
