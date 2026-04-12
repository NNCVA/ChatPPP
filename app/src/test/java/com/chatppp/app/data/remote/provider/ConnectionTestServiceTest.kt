package com.chatppp.app.data.remote.provider

import kotlinx.coroutines.test.runTest
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

class ConnectionTestServiceTest {
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
    fun testConnection_success_returns_success() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""")
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val result = service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "test-key",
            relayToken = null,
            isRelay = false
        )

        assertTrue(result is ConnectionTestResult.Success)
    }

    @Test
    fun testConnection_with_bearer_auth_sends_authorization_header() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""")
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "my-secret-key",
            relayToken = null,
            isRelay = false
        )

        val request = server.takeRequest()
        assertEquals("Bearer my-secret-key", request.getHeader("Authorization"))
    }

    @Test
    fun testConnection_with_relay_token_sends_x_relay_token_header() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""")
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "test-model",
            apiKey = null,
            relayToken = "relay-secret-token",
            isRelay = true
        )

        val request = server.takeRequest()
        assertEquals("relay-secret-token", request.getHeader("X-Relay-Token"))
    }

    @Test
    fun testConnection_401_returns_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"Unauthorized"}}""")
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val result = service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "bad-key",
            relayToken = null,
            isRelay = false
        )

        assertTrue(result is ConnectionTestResult.Failure)
        assertEquals("Error 401: Client Error", (result as ConnectionTestResult.Failure).message)
    }

    @Test
    fun testConnection_404_returns_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error":{"message":"Not Found"}}""")
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val result = service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "test-key",
            relayToken = null,
            isRelay = false
        )

        assertTrue(result is ConnectionTestResult.Failure)
        assertEquals("Error 404: Client Error", (result as ConnectionTestResult.Failure).message)
    }

    @Test
    fun testConnection_timeout_returns_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val result = service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "test-key",
            relayToken = null,
            isRelay = false
        )

        assertTrue(result is ConnectionTestResult.Failure)
    }

    @Test
    fun blank_model_uses_same_default_as_runtime_requests() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""")
        )

        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val result = service.testConnection(
            baseUrl = server.url("/v1").toString(),
            model = "",
            apiKey = "test-key",
            relayToken = null,
            isRelay = false
        )

        val requestBody = server.takeRequest().body.readUtf8()

        assertTrue(result is ConnectionTestResult.Success)
        assertTrue(requestBody.contains("\"model\":\"deepseek-chat\""))
    }

    @Test
    fun full_chat_completions_url_is_rejected_before_sending_request() = runTest {
        val service = ConnectionTestService(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        val result = service.testConnection(
            baseUrl = "https://api.example.com/v1/chat/completions",
            model = "test-model",
            apiKey = "test-key",
            relayToken = null,
            isRelay = false
        )

        assertTrue(result is ConnectionTestResult.Failure)
        assertEquals(
            "OpenAI Base URL must not include /chat/completions",
            (result as ConnectionTestResult.Failure).message
        )
        assertEquals(0, server.requestCount)
    }
}
