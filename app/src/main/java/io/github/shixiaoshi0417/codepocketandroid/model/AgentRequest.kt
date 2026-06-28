package io.github.shixiaoshi0417.codepocketandroid.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentRequest(
    val type: String,
    val prompt: String,
    val model: String = "",
    val sessionId: String = ""
)
