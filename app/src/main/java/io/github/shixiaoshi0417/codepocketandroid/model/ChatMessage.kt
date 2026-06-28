package io.github.shixiaoshi0417.codepocketandroid.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val conversationId: String = "default",
    val messageType: MessageType = MessageType.CHAT,
    val agentSessionId: String = ""
)
