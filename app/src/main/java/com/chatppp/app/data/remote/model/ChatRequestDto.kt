package com.chatppp.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

@Serializable
data class ChatResponseDto(
    val choices: List<ChatChoiceDto>
)

@Serializable
data class ChatChoiceDto(
    val message: ChatMessageDto? = null,
    val delta: StreamDeltaDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class StreamChunkDto(
    val choices: List<StreamChoiceDto>
)

@Serializable
data class StreamChoiceDto(
    val delta: StreamDeltaDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class StreamDeltaDto(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null
)
