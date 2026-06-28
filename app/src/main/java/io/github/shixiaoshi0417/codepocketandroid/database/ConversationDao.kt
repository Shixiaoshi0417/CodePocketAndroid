package io.github.shixiaoshi0417.codepocketandroid.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.shixiaoshi0417.codepocketandroid.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY updated_at DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updated_at DESC")
    suspend fun getAllConversationsOnce(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("UPDATE conversations SET title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateConversation(id: String, title: String, updatedAt: Long)
}
