package com.chatppp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_summaries")
data class ConversationSummaryEntity(
    @PrimaryKey val conversationId: String,
    val summaryText: String,
    val coveredUntilMessageId: String?,
    val coveredUntilCreatedAt: Long,
    val updatedAt: Long
)
