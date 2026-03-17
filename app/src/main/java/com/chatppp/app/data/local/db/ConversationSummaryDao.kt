package com.chatppp.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chatppp.app.data.local.entity.ConversationSummaryEntity

@Dao
interface ConversationSummaryDao {
    @Query("SELECT * FROM conversation_summaries WHERE conversationId = :conversationId LIMIT 1")
    suspend fun getByConversationId(conversationId: String): ConversationSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: ConversationSummaryEntity)

    @Query("DELETE FROM conversation_summaries WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)
}
