package io.github.shixiaoshi0417.codepocketandroid.model

import java.util.UUID

data class AgentStep(
    val id: String = UUID.randomUUID().toString(),
    val type: AgentStatusType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
