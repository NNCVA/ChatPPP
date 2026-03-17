package com.chatppp.app.data.remote.provider

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.remote.model.ChatMessageDto
import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.parser.ChatStreamParser
import com.chatppp.app.domain.model.ChatError
import com.chatppp.app.domain.model.ProviderType
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSelectorTest {
    private val scopes = mutableListOf<CoroutineScope>()
    private val servers = mutableListOf<MockWebServer>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
        servers.forEach { it.shutdown() }
        servers.clear()
    }

    @Test
    fun direct_mode_uses_runtime_base_url_and_api_key() = runTest {
        val server = startedServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"choices":[{"message":{"role":"assistant","content":"Hello direct"}}]}""")
            )
        }
        val appPreferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = server.url("/v1").toString().removeSuffix("/"),
                model = "deepseek-chat",
                streamEnabled = false
            )
        }
        val secretStore = FakeRuntimeSecretStore().apply {
            saveDirectApiKey("runtime-direct-key")
        }

        val selector = RuntimeProviderSelector(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            appPreferences = appPreferences,
            secretStore = secretStore
        )

        val provider = selector.select(ProviderType.DIRECT)
        val result = provider.send(chatRequest(stream = false))
        val request = server.takeRequest()

        assertEquals("Hello direct", result)
        assertEquals("Bearer runtime-direct-key", request.getHeader("Authorization"))
        assertEquals("/v1/chat/completions", request.path)
    }

    @Test
    fun relay_mode_uses_runtime_base_url_and_token() = runTest {
        val server = startedServer().apply {
            enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"choices":[{"message":{"role":"assistant","content":"Hello relay"}}]}""")
            )
        }
        val appPreferences = createPreferences().also {
            it.updateProviderAndChatSettings(
                providerType = ProviderType.RELAY,
                baseUrl = server.url("/v1").toString().removeSuffix("/"),
                model = "deepseek-chat",
                streamEnabled = false
            )
        }
        val secretStore = FakeRuntimeSecretStore().apply {
            saveRelayToken("runtime-relay-token")
        }

        val selector = RuntimeProviderSelector(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            appPreferences = appPreferences,
            secretStore = secretStore
        )

        val provider = selector.select(ProviderType.RELAY)
        val result = provider.send(chatRequest(stream = false))
        val request = server.takeRequest()

        assertEquals("Hello relay", result)
        assertEquals("runtime-relay-token", request.getHeader("X-Relay-Token"))
        assertEquals("/v1/chat/completions", request.path)
    }

    @Test
    fun direct_mode_without_api_key_throws_config_error() = runTest {
        val selector = RuntimeProviderSelector(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            appPreferences = createPreferences(),
            secretStore = FakeRuntimeSecretStore()
        )

        val result = runCatching { selector.select(ProviderType.DIRECT) }

        assertTrue(result.exceptionOrNull() is ChatError.Config)
    }

    @Test
    fun relay_mode_without_token_throws_backend_specific_config_error() = runTest {
        val appPreferences = createPreferences().also {
            it.updateProviderAndChatSettings(
                providerType = ProviderType.RELAY,
                baseUrl = "https://relay.example.com/v1",
                model = "deepseek-chat",
                streamEnabled = false
            )
        }

        val selector = RuntimeProviderSelector(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            appPreferences = appPreferences,
            secretStore = FakeRuntimeSecretStore()
        )

        val result = runCatching { selector.select(ProviderType.RELAY) }

        assertTrue(result.exceptionOrNull() is ChatError.Config)
        assertEquals(
            "Relay mode requires your own backend relay token",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun full_chat_completions_url_is_rejected_as_invalid_base_url() = runTest {
        val appPreferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://apis.iflow.cn/v1/chat/completions",
                model = "deepseek-chat",
                streamEnabled = false
            )
        }

        val selector = RuntimeProviderSelector(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            streamParser = ChatStreamParser(),
            appPreferences = appPreferences,
            secretStore = FakeRuntimeSecretStore().apply {
                saveDirectApiKey("runtime-direct-key")
            }
        )

        val result = runCatching { selector.select(ProviderType.DIRECT) }

        assertTrue(result.exceptionOrNull() is ChatError.Config)
        assertEquals(
            "OpenAI Base URL must not include /chat/completions",
            result.exceptionOrNull()?.message
        )
    }

    private fun createPreferences(): AppPreferences {
        val directory = Files.createTempDirectory("chatppp-provider-selector-test").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(directory, "app.preferences_pb") }
        )
        return AppPreferences(dataStore)
    }

    private fun startedServer(): MockWebServer = MockWebServer().also {
        it.start()
        servers += it
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

private class FakeRuntimeSecretStore : SecretStore {
    private var directApiKey: String? = null
    private var relayToken: String? = null

    override fun getDirectApiKey(): String? = directApiKey

    override fun saveDirectApiKey(value: String) {
        directApiKey = value
    }

    override fun clearDirectApiKey() {
        directApiKey = null
    }

    override fun getRelayToken(): String? = relayToken

    override fun saveRelayToken(value: String) {
        relayToken = value
    }

    override fun clearRelayToken() {
        relayToken = null
    }
}
