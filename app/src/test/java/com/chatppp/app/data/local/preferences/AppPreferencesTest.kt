package com.chatppp.app.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatppp.app.domain.model.ProviderType
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesTest {
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun default_provider_is_direct() = runTest {
        val preferences = createPreferences()

        assertEquals(ProviderType.DIRECT, preferences.providerType.first())
    }

    @Test
    fun base_url_model_and_stream_enabled_round_trip() = runTest {
        val preferences = createPreferences()

        preferences.updateChatSettings(
            baseUrl = "https://api.example.com",
            model = "deepseek-chat",
            streamEnabled = false
        )

        assertEquals("https://api.example.com", preferences.baseUrl.first())
        assertEquals("deepseek-chat", preferences.model.first())
        assertFalse(preferences.streamEnabled.first())
    }

    @Test
    fun last_conversation_id_round_trips() = runTest {
        val preferences = createPreferences()

        assertNull(preferences.lastConversationId.first())

        preferences.setLastConversationId("conversation-7")
        assertEquals("conversation-7", preferences.lastConversationId.first())
    }

    @Test
    fun summary_compression_settings_round_trip() = runTest {
        val preferences = createPreferences()

        assertTrue(preferences.summaryCompressionEnabled.first())

        preferences.setSummaryCompressionEnabled(false)

        assertFalse(preferences.summaryCompressionEnabled.first())
    }

    @Test
    fun config_preset_metadata_round_trip() = runTest {
        val preferences = createPreferences()

        preferences.setConfigPresetMetadata(
            listOf(
                ConfigPresetMetadata(
                    id = "preset-1",
                    name = "Preset 1",
                    providerType = ProviderType.DIRECT,
                    baseUrl = "https://api.openai.com/v1",
                    model = "gpt-4.1-mini",
                    streamEnabled = true
                )
            )
        )

        assertEquals(
            listOf(
                ConfigPresetMetadata(
                    id = "preset-1",
                    name = "Preset 1",
                    providerType = ProviderType.DIRECT,
                    baseUrl = "https://api.openai.com/v1",
                    model = "gpt-4.1-mini",
                    streamEnabled = true
                )
            ),
            preferences.configPresetMetadata.first()
        )
    }

    private fun createPreferences(): AppPreferences {
        val directory = Files.createTempDirectory("chatppp-preferences").toFile()
        val file = File(directory, "app.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
        return AppPreferences(dataStore)
    }
}
