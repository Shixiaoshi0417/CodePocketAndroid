package io.github.shixiaoshi0417.codepocketandroid.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val type: String = "",
    val role: String = "",
    val content: String = "",
    val message: String = "",
    val success: Boolean? = null
)
