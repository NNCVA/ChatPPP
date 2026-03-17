package com.chatppp.app.data.remote.parser

import com.chatppp.app.domain.model.ChatError
import com.chatppp.app.domain.model.ChatChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamParserTest {
    @Test
    fun parse_lines_returns_content_chunks_in_order() {
        val parser = ChatStreamParser()

        val chunks = parser.parseLines(
            listOf(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}",
                "data: [DONE]"
            )
        )

        assertEquals(
            listOf(
                ChatChunk.Content("Hel"),
                ChatChunk.Content("lo"),
                ChatChunk.Done
            ),
            chunks
        )
    }

    @Test
    fun parse_lines_accepts_iflow_style_data_lines_without_space_after_colon() {
        val parser = ChatStreamParser()

        val chunks = parser.parseLines(
            listOf(
                "data:{\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}",
                "data:{\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}",
                "data:[DONE]"
            )
        )

        assertEquals(
            listOf(
                ChatChunk.Content("Hel"),
                ChatChunk.Content("lo"),
                ChatChunk.Done
            ),
            chunks
        )
    }

    @Test
    fun parse_lines_returns_stream_protocol_error_for_bad_json() {
        val parser = ChatStreamParser()

        val result = runCatching {
            parser.parseLines(
                listOf("data: {not-json}")
            )
        }

        assertTrue(result.exceptionOrNull() is ChatError.StreamProtocol)
    }

    @Test
    fun parse_lines_distinguishes_answer_text_from_thinking_text() {
        val parser = ChatStreamParser()

        val chunks = parser.parseLines(
            listOf(
                "data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"Think\"}}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\"Answer\"}}]}",
                "data: [DONE]"
            )
        )

        assertEquals(
            listOf(
                ChatChunk.Thinking("Think"),
                ChatChunk.Content("Answer"),
                ChatChunk.Done
            ),
            chunks
        )
    }
}
