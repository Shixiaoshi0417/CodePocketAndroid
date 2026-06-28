package io.github.shixiaoshi0417.codepocketandroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "is_streaming")
    val isStreaming: Boolean = false
)
