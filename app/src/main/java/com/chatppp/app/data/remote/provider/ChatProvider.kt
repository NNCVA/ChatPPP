package com.chatppp.app.data.remote.provider

import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.domain.model.ChatChunk
import kotlinx.coroutines.flow.Flow

interface ChatProvider {
    suspend fun send(request: ChatRequestDto): String

    fun stream(request: ChatRequestDto): Flow<ChatChunk>
}
