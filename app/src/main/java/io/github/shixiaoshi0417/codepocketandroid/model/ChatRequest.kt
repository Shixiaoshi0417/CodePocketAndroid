package io.github.shixiaoshi0417.codepocketandroid.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val type: String = "chat",
    val message: String
)
