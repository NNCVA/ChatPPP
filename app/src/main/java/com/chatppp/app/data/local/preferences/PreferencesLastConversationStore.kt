package com.chatppp.app.data.local.preferences

import com.chatppp.app.domain.session.LastConversationStore
import kotlinx.coroutines.flow.Flow

class PreferencesLastConversationStore(
    private val appPreferences: AppPreferences
) : LastConversationStore {
    override val lastConversationId: Flow<String?> = appPreferences.lastConversationId

    override suspend fun setLastConversationId(conversationId: String) {
        appPreferences.setLastConversationId(conversationId)
    }

    override suspend fun clearLastConversationId() {
        appPreferences.clearLastConversationId()
    }
}
