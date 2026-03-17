package com.chatppp.app.data.remote.provider

import com.chatppp.app.data.remote.model.ChatMessageDto
import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.parser.ChatStreamParser
import com.chatppp.app.domain.model.ChatChunk
import com.chatppp.app.domain.model.ChatError
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DirectApiProviderTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun send_uses_bearer_auth_and_returns_message_content() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"Hello"}}]}""")
        )

        val provider = DirectApiProvider(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            endpointUrl = server.url("/v1/chat/completions").toString(),
            apiKey = "direct-key"
        )

        val result = provider.send(chatRequest(stream = false))
        val request = server.takeRequest()

        assertEquals("Hello", result)
        assertEquals("/v1/chat/completions", request.path)
        assertEquals("Bearer direct-key", request.getHeader("Authorization"))
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
    }

    @Test
    fun stream_emits_content_chunks_then_done() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    """
                    data: {"choices":[{"delta":{"content":"Hel"}}]}
                    data: {"choices":[{"delta":{"content":"lo"}}]}
                    data: [DONE]
                    
                    """.trimIndent()
                )
        )

        val provider = DirectApiProvider(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            endpointUrl = server.url("/v1/chat/completions").toString(),
            apiKey = "direct-key"
        )

        val chunks = provider.stream(chatRequest(stream = true)).toList()

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
    fun stream_accepts_iflow_style_data_lines_without_space_after_colon() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    """
                    data:{"choices":[{"delta":{"content":"Hel"}}]}
                    data:{"choices":[{"delta":{"content":"lo"}}]}
                    data:[DONE]

                    """.trimIndent()
                )
        )

        val provider = DirectApiProvider(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            endpointUrl = server.url("/v1/chat/completions").toString(),
            apiKey = "direct-key"
        )

        val chunks = provider.stream(chatRequest(stream = true)).toList()

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
    fun stream_completes_after_done_even_if_server_keeps_connection_open() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setSocketPolicy(SocketPolicy.KEEP_OPEN)
                .setBody(
                    """
                    data: {"choices":[{"delta":{"content":"Hel"}}]}
                    data: {"choices":[{"delta":{"content":"lo"}}]}
                    data: [DONE]
                    
                    """.trimIndent()
                )
        )

        val provider = DirectApiProvider(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            endpointUrl = server.url("/v1/chat/completions").toString(),
            apiKey = "direct-key"
        )

        val chunks = withTimeout(1_000) {
            provider.stream(chatRequest(stream = true)).toList()
        }

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
    fun send_maps_401_to_auth_error() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"Unauthorized"}}""")
        )

        val provider = DirectApiProvider(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            endpointUrl = server.url("/v1/chat/completions").toString(),
            apiKey = "bad-key"
        )

        val result = runCatching { provider.send(chatRequest(stream = false)) }

        assertTrue(result.exceptionOrNull() is ChatError.Auth)
    }

    private fun chatRequest(stream: Boolean): ChatRequestDto =
        ChatRequestDto(
            model = "deepseek-chat",
            messages = listOf(
                ChatMessageDto(
                    role = "user",
                    content = "Hello"
                )
            ),
            stream = stream
        )
}
