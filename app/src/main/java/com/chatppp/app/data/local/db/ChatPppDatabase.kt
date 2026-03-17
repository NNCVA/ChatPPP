package com.chatppp.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chatppp.app.data.local.entity.ConversationEntity
import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, ConversationSummaryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class ChatPppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao

    abstract fun conversationSummaryDao(): ConversationSummaryDao
}
