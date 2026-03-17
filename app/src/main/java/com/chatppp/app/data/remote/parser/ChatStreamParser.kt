package com.chatppp.app.data.remote.parser

import com.chatppp.app.data.remote.model.StreamChunkDto
import com.chatppp.app.domain.model.ChatChunk
import com.chatppp.app.domain.model.ChatError
import kotlinx.serialization.json.Json

class ChatStreamParser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun parseLines(lines: List<String>): List<ChatChunk> =
        lines.mapNotNull(::parseLine)

    fun parseLine(line: String): ChatChunk? {
        if (!line.startsWith("data:")) {
            return null
        }

        val payload = line.removePrefix("data:").trimStart()
        if (payload.isEmpty()) {
            return null
        }
        if (payload == "[DONE]") {
            return ChatChunk.Done
        }

        val chunk = try {
            json.decodeFromString(StreamChunkDto.serializer(), payload)
        } catch (error: Exception) {
            throw ChatError.StreamProtocol(error)
        }

        val delta = chunk.choices.firstOrNull()?.delta
        val thinkingText = delta?.reasoningContent
        if (!thinkingText.isNullOrEmpty()) {
            return ChatChunk.Thinking(thinkingText)
        }

        val text = delta?.content
        return text?.let(ChatChunk::Content)
    }
}
