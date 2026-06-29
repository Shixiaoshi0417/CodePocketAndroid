package io.github.shixiaoshi0417.codepocketandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.shixiaoshi0417.codepocketandroid.database.AppDatabase
import io.github.shixiaoshi0417.codepocketandroid.database.entity.MessageEntity
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.model.MessageRole
import io.github.shixiaoshi0417.codepocketandroid.model.MessageType
import io.github.shixiaoshi0417.codepocketandroid.model.OpenCodeSession
import io.github.shixiaoshi0417.codepocketandroid.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val messageDao = db.messageDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()
    private val sessionJson = Json { ignoreUnknownKeys = true }

    private val _selectedSessionId = MutableStateFlow("")
    val selectedSessionId: StateFlow<String> = _selectedSessionId.asStateFlow()

    private val _isAgentProcessing = MutableStateFlow(false)

    private val webSocketManager = WebSocketManager(
        onMessagePersist = { message ->
            messageDao.insertMessage(MessageEntity(
                id = message.id, role = message.role.name, content = message.content,
                timestamp = message.timestamp, conversationId = message.agentSessionId.ifEmpty { _selectedSessionId.value },
                isStreaming = message.isStreaming, messageType = message.messageType.name,
                agentSessionId = message.agentSessionId.ifEmpty { _selectedSessionId.value }
            ))
        },
        onProcessingChange = { processing ->
            _isAgentProcessing.value = processing
        }
    )

    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    val messages: StateFlow<List<ChatMessage>> = webSocketManager.messages
    val agentViewModel = AgentViewModel(webSocketManager, _isAgentProcessing)

    private val _sessions = MutableStateFlow<List<OpenCodeSession>>(emptyList())
    val sessions: StateFlow<List<OpenCodeSession>> = _sessions.asStateFlow()

    val currentModel: StateFlow<String> = combine(_sessions, _selectedSessionId) { sessions, id ->
        sessions.find { it.id == id }?.model?.ifEmpty { null } ?: "deepseek-v4-pro"
    }.stateIn(scope, SharingStarted.Lazily, "deepseek-v4-pro")

    val currentSession: StateFlow<OpenCodeSession?> = combine(_sessions, _selectedSessionId) { sessions, id ->
        sessions.find { it.id == id }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val availableModels = listOf("deepseek-v4-pro", "deepseek-v4-flash", "gpt-5", "claude-sonnet-4", "gemini-2.5-pro")

    init {
        scope.launch {
            val fixed = messageDao.fixMessageTypes()
            android.util.Log.i("CodePocket", "Fixed $fixed mislabeled messages")
            val allMsgs = messageDao.getAllMessages()
            allMsgs.forEach { msg ->
                android.util.Log.i("CodePocket", "  DB [${msg.messageType}] role=${msg.role} id=${msg.id.take(20)} stream=${msg.isStreaming} session=${msg.agentSessionId.take(20)} content=${msg.content.take(50)}")
            }
            loadSessions()
            android.util.Log.i("CodePocket", "Loaded ${_sessions.value.size} sessions")
            _sessions.value.forEach { s ->
                val isTest = s.title.contains("Say hello", ignoreCase = true) ||
                        s.title.contains("List Kotlin", ignoreCase = true) ||
                        s.title.contains("Example", ignoreCase = true) ||
                        s.title.contains("Demo", ignoreCase = true) ||
                        s.title.contains("Test", ignoreCase = true)
                android.util.Log.i("CodePocket", "  [${s.id.take(20)}] ${s.title} ${if (isTest) "<<< TEST" else ""}")
            }
        }
    }

    fun loadSessions() {
        scope.launch {
            try {
                val req = Request.Builder().url("http://127.0.0.1:8765/sessions").build()
                val body = httpClient.newCall(req).execute().body?.string() ?: ""
                _sessions.value = sessionJson.parseToJsonElement(body).jsonArray.map {
                    val o = it.jsonObject
                    OpenCodeSession(
                        id = o["id"]?.jsonPrimitive?.content ?: "",
                        title = o["title"]?.jsonPrimitive?.content ?: "",
                        directory = o["directory"]?.jsonPrimitive?.content ?: "",
                        agent = o["agent"]?.jsonPrimitive?.content ?: "",
                        model = o["model"]?.jsonPrimitive?.content ?: "deepseek-v4-pro",
                        timeUpdated = o["time_updated"]?.jsonPrimitive?.long ?: 0L
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun openSession(sessionId: String) {
        _selectedSessionId.value = sessionId
        webSocketManager.conversationId = sessionId
        if (sessionId.isEmpty()) {
            webSocketManager.switchConversation("", emptyList())
            return
        }
        scope.launch {
            android.util.Log.d("LOAD", "openSession: $sessionId")
            val localMessages = loadLocalMessages(sessionId)
            android.util.Log.d("LOAD", "localMessages count: ${localMessages.size}")
            localMessages.forEach {
                android.util.Log.d("DB", "${it.id.take(20)} ${it.role} type=${it.messageType} ${it.content.take(50)}")
            }
            webSocketManager.switchConversation(sessionId, localMessages)
            val uiMessages = webSocketManager.messages.value
            android.util.Log.d("UI", "uiMessages after switch: ${uiMessages.size}")
            uiMessages.forEach {
                android.util.Log.d("UI", "${it.id.take(20)} ${it.role} type=${it.messageType} ${it.content.take(50)}")
            }
        }
    }

    private suspend fun loadLocalMessages(sessionId: String): List<ChatMessage> {
        return try {
            val entities = messageDao.getMessagesBySession(sessionId)
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = try { MessageRole.valueOf(entity.role) } catch (e: Exception) { MessageRole.ASSISTANT },
                    content = entity.content,
                    timestamp = entity.timestamp,
                    isStreaming = false,
                    conversationId = entity.conversationId,
                    messageType = try { MessageType.valueOf(entity.messageType) } catch (e: Exception) { MessageType.CHAT },
                    agentSessionId = entity.agentSessionId
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadMessages(sessionId: String): List<ChatMessage> {
        return try {
            val req = Request.Builder()
                .url("http://127.0.0.1:8765/sessions/$sessionId/messages")
                .build()
            val body = httpClient.newCall(req).execute().body?.string() ?: "[]"
            sessionJson.parseToJsonElement(body).jsonArray.flatMap { elem ->
                val o = elem.jsonObject
                val roleStr = o["role"]?.jsonPrimitive?.content?.uppercase() ?: "USER"
                val role = try { MessageRole.valueOf(roleStr) } catch (_: Exception) { MessageRole.USER }
                val parts = o["parts"]?.jsonArray ?: return@flatMap emptyList()
                val timeCreated = o["time_created"]?.jsonPrimitive?.long ?: System.currentTimeMillis()
                val msgId = o["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()

                val toolContent = parts.mapNotNull { p ->
                    val obj = p.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    val text = obj["text"]?.jsonPrimitive?.content ?: ""
                    if (type in listOf("tool", "step-start", "step-finish")) text.ifEmpty { null } else null
                }.joinToString("\n").trim()

                val textContent = parts.mapNotNull { p ->
                    val obj = p.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    val text = obj["text"]?.jsonPrimitive?.content ?: ""
                    if (type == "text") text.ifEmpty { null } else null
                }.joinToString("\n").trim()

                val result = mutableListOf<ChatMessage>()
                if (toolContent.isNotEmpty()) {
                    result.add(ChatMessage(
                        id = "${msgId}_proc", role = role, content = toolContent,
                        timestamp = timeCreated, conversationId = sessionId,
                        messageType = MessageType.AGENT_STATUS
                    ))
                }
                if (textContent.isNotEmpty()) {
                    result.add(ChatMessage(
                        id = "${msgId}_text", role = role, content = textContent,
                        timestamp = timeCreated, conversationId = sessionId,
                        messageType = MessageType.CHAT
                    ))
                }
                if (result.isEmpty() && role == MessageRole.USER) {
                    val content = parts.joinToString("\n") {
                        it.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                    }.trim()
                    if (content.isNotEmpty()) {
                        result.add(ChatMessage(
                            id = msgId, role = role, content = content,
                            timestamp = timeCreated, conversationId = sessionId,
                            messageType = MessageType.CHAT
                        ))
                    }
                }
                result
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun newSession() {
        scope.launch {
            try {
                val json = """{"title":"New Chat","model":"deepseek-v4-pro"}"""
                val req = Request.Builder().url("http://127.0.0.1:8765/sessions")
                    .post(json.toRequestBody("application/json".toMediaType())).build()
                val body = httpClient.newCall(req).execute().body?.string() ?: "{}"
                val o = sessionJson.parseToJsonElement(body).jsonObject
                val id = o["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                loadSessions()
                openSession(id)
            } catch (_: Exception) {
                val id = UUID.randomUUID().toString()
                _sessions.value = _sessions.value + OpenCodeSession(id = id, title = "New Chat", timeUpdated = System.currentTimeMillis(), directory = "", agent = "", model = "deepseek-v4-pro")
                openSession(id)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        scope.launch {
            try {
                val req = Request.Builder().url("http://127.0.0.1:8765/sessions/$sessionId").delete().build()
                httpClient.newCall(req).execute()
            } catch (_: Exception) {}
            loadSessions()
            val remaining = _sessions.value
            if (remaining.isNotEmpty() && _selectedSessionId.value == sessionId) {
                openSession(remaining.first().id)
            }
        }
    }

    fun connect() { webSocketManager.connect() }
    fun disconnect() { webSocketManager.disconnect() }

    fun clearTestData() {
        scope.launch {
            try {
                val req = Request.Builder()
                    .url("http://127.0.0.1:8765/admin/clear-test-data")
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(req).execute()
                loadSessions()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
