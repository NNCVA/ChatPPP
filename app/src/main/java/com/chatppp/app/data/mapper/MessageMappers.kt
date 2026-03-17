package com.chatppp.app.data.mapper

import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus

fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.valueOf(role),
        content = content,
        thinkingContent = thinkingContent,
        status = MessageStatus.valueOf(status),
        createdAt = createdAt
    )
