package com.chatppp.app.data.remote.provider

import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.remote.parser.ChatStreamParser
import com.chatppp.app.domain.model.ChatError
import com.chatppp.app.domain.model.ProviderType
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

interface ProviderSelector {
    suspend fun select(providerType: ProviderType): ChatProvider
}

class RuntimeProviderSelector @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val streamParser: ChatStreamParser,
    private val appPreferences: AppPreferences,
    private val secretStore: SecretStore
) : ProviderSelector {
    override suspend fun select(providerType: ProviderType): ChatProvider {
        val endpointUrl = appPreferences.baseUrl.first().trim().toChatCompletionsEndpoint()
        if (endpointUrl.isEmpty()) {
            throw ChatError.Config("Base URL is required before sending chat requests")
        }

        return when (providerType) {
            ProviderType.DIRECT -> {
                val apiKey = secretStore.getDirectApiKey().orEmpty().trim()
                if (apiKey.isEmpty()) {
                    throw ChatError.Config("Direct mode requires an API key")
                }
                DirectApiProvider(
                    okHttpClient = okHttpClient,
                    json = json,
                    streamParser = streamParser,
                    endpointUrl = endpointUrl,
                    apiKey = apiKey
                )
            }

            ProviderType.RELAY -> {
                val relayToken = secretStore.getRelayToken().orEmpty().trim()
                if (relayToken.isEmpty()) {
                    throw ChatError.Config("Relay mode requires your own backend relay token")
                }
                RelayApiProvider(
                    okHttpClient = okHttpClient,
                    json = json,
                    streamParser = streamParser,
                    endpointUrl = endpointUrl,
                    relayToken = relayToken
                )
            }
        }
    }

    private fun String.toChatCompletionsEndpoint(): String {
        if (isEmpty()) {
            return ""
        }

        val normalized = removeSuffix("/")
        if (normalized.endsWith("/chat/completions")) {
            throw ChatError.Config("OpenAI Base URL must not include /chat/completions")
        }

        return "$normalized/chat/completions"
    }
}
