package com.chatppp.app.data.context

import com.chatppp.app.data.local.db.ConversationSummaryDao
import com.chatppp.app.data.local.entity.ConversationSummaryEntity

interface ConversationSummaryStore {
    suspend fun get(conversationId: String): ConversationSummaryEntity?

    suspend fun upsert(summary: ConversationSummaryEntity)

    suspend fun delete(conversationId: String)
}

class DefaultConversationSummaryStore(
    private val dao: ConversationSummaryDao
) : ConversationSummaryStore {
    override suspend fun get(conversationId: String): ConversationSummaryEntity? =
        dao.getByConversationId(conversationId)

    override suspend fun upsert(summary: ConversationSummaryEntity) {
        dao.upsert(summary)
    }

    override suspend fun delete(conversationId: String) {
        dao.deleteByConversationId(conversationId)
    }
}

object NoOpConversationSummaryStore : ConversationSummaryStore {
    override suspend fun get(conversationId: String): ConversationSummaryEntity? = null

    override suspend fun upsert(summary: ConversationSummaryEntity) = Unit

    override suspend fun delete(conversationId: String) = Unit
}
