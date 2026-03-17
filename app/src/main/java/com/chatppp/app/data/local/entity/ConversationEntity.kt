package com.chatppp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val providerType: String,
    val presetId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
