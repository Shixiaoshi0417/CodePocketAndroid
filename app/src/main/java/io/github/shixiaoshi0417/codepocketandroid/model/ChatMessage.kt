package io.github.shixiaoshi0417.codepocketandroid.model

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
)
