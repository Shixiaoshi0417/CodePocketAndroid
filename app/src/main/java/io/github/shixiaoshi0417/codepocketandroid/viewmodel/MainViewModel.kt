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
            loadSessions()
            val count = _sessions.value.size
            android.util.Log.i("CodePocket", "Loaded $count sessions")
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
        webSocketManager.switchConversation(sessionId, emptyList())
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
