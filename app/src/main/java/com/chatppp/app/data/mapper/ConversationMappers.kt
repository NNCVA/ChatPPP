package com.chatppp.app.data.mapper

import com.chatppp.app.data.local.entity.ConversationEntity
import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.ProviderType

fun ConversationEntity.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        providerType = ProviderType.valueOf(providerType),
        presetId = presetId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
