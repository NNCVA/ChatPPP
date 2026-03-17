package com.chatppp.app.domain.model

sealed interface ChatChunk {
    data class Thinking(val text: String) : ChatChunk

    data class Content(val text: String) : ChatChunk

    data object Done : ChatChunk
}
