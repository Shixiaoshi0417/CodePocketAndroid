package io.github.shixiaoshi0417.codepocketandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.shixiaoshi0417.codepocketandroid.database.AppDatabase
import io.github.shixiaoshi0417.codepocketandroid.database.entity.ConversationEntity
import io.github.shixiaoshi0417.codepocketandroid.database.entity.MessageEntity
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.model.Conversation
import io.github.shixiaoshi0417.codepocketandroid.model.MessageRole
import io.github.shixiaoshi0417.codepocketandroid.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = application

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow("")
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val webSocketManager = WebSocketManager(
        onMessagePersist = { message ->
            messageDao.insertMessage(
                MessageEntity(
                    id = message.id,
                    role = message.role.name,
                    content = message.content,
                    timestamp = message.timestamp,
                    conversationId = message.conversationId,
                    isStreaming = message.isStreaming
                )
            )
            updateConversationTimestamp(message.conversationId)
            autoNameConversation(message)
        }
    )

    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    val messages: StateFlow<List<ChatMessage>> = webSocketManager.messages

    init {
        scope.launch {
            initializeConversations()
        }
    }

    private suspend fun initializeConversations() {
        val entities = conversationDao.getAllConversationsOnce()
        if (entities.isEmpty()) {
            val newConv = ConversationEntity(
                id = UUID.randomUUID().toString(),
                title = "New Chat",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            conversationDao.insertConversation(newConv)
            _currentConversationId.value = newConv.id
            webSocketManager.switchConversation(newConv.id, emptyList())
            refreshConversations()
        } else {
            val latest = entities.first()
            _currentConversationId.value = latest.id
            loadMessagesForConversation(latest.id)
            refreshConversations()
        }
    }

    private fun loadMessagesForConversation(convId: String) {
        scope.launch {
            val entities = messageDao.getMessagesByConversationOnce(convId)
            val restored = entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = try { MessageRole.valueOf(entity.role) } catch (e: Exception) { MessageRole.ASSISTANT },
                    content = entity.content,
                    timestamp = entity.timestamp,
                    isStreaming = false,
                    conversationId = entity.conversationId
                )
            }
            webSocketManager.switchConversation(convId, restored)
        }
    }

    private suspend fun refreshConversations() {
        val entities = conversationDao.getAllConversationsOnce()
        val convs = entities.map { entity ->
            val count = messageDao.getMessageCount(entity.id)
            Conversation(
                id = entity.id,
                title = entity.title,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                messageCount = count
            )
        }
        _conversations.value = convs
    }

    private suspend fun updateConversationTimestamp(convId: String) {
        val conv = conversationDao.getConversation(convId) ?: return
        conversationDao.updateConversation(convId, conv.title, System.currentTimeMillis())
    }

    private fun autoNameConversation(message: ChatMessage) {
        if (message.role != MessageRole.USER) return
        scope.launch {
            val conv = conversationDao.getConversation(message.conversationId) ?: return@launch
            if (conv.title != "New Chat") return@launch
            val title = if (message.content.length > 20) {
                message.content.take(20)
            } else {
                message.content
            }
            conversationDao.updateConversation(conv.id, title, System.currentTimeMillis())
            refreshConversations()
        }
    }

    fun connect() {
        webSocketManager.connect()
    }

    fun disconnect() {
        webSocketManager.disconnect()
    }

    fun sendChat(message: String) {
        webSocketManager.sendChat(message)
    }

    fun newConversation() {
        scope.launch {
            val conv = ConversationEntity(
                id = UUID.randomUUID().toString(),
                title = "New Chat",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            conversationDao.insertConversation(conv)
            _currentConversationId.value = conv.id
            webSocketManager.switchConversation(conv.id, emptyList())
            refreshConversations()
        }
    }

    fun switchConversation(convId: String) {
        _currentConversationId.value = convId
        loadMessagesForConversation(convId)
        scope.launch {
            refreshConversations()
        }
    }

    fun deleteConversation(convId: String) {
        scope.launch {
            conversationDao.deleteConversation(convId)
            messageDao.deleteMessagesByConversation(convId)
            val remaining = conversationDao.getAllConversationsOnce()
            if (remaining.isEmpty()) {
                val conv = ConversationEntity(
                    id = UUID.randomUUID().toString(),
                    title = "New Chat",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                conversationDao.insertConversation(conv)
                _currentConversationId.value = conv.id
                webSocketManager.switchConversation(conv.id, emptyList())
            } else if (_currentConversationId.value == convId) {
                val latest = remaining.first()
                _currentConversationId.value = latest.id
                loadMessagesForConversation(latest.id)
            }
            refreshConversations()
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
