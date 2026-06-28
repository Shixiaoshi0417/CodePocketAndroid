package io.github.shixiaoshi0417.codepocketandroid.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
