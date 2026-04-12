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

    fun validate(
        providerType: ProviderType,
        baseUrl: String,
        directApiKey: String?,
        relayToken: String?
    ): SettingsValidationResult
}

data class SettingsValidationResult(
    val baseUrlError: String? = null,
    val modelError: String? = null,
    val credentialError: String? = null,
    val readinessLabel: String = "Not ready"
) {
    companion object {
        fun ready() = SettingsValidationResult(readinessLabel = "Ready")
        fun notReady(vararg errors: Pair<String, String>): SettingsValidationResult {
            val baseUrlError = errors.find { it.first == "baseUrl" }?.second
            val modelError = errors.find { it.first == "model" }?.second
            val credentialError = errors.find { it.first == "credential" }?.second
            val hasErrors = baseUrlError != null || modelError != null || credentialError != null
            return SettingsValidationResult(
                baseUrlError = baseUrlError,
                modelError = modelError,
                credentialError = credentialError,
                readinessLabel = if (hasErrors) "Not ready" else "Ready"
            )
        }
    }
}

class RuntimeProviderSelector @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val streamParser: ChatStreamParser,
    private val appPreferences: AppPreferences,
    private val secretStore: SecretStore
) : ProviderSelector {
    override suspend fun select(providerType: ProviderType): ChatProvider {
        val endpointUrl = normalizeChatCompletionsEndpoint(appPreferences.baseUrl.first())
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

    override fun validate(
        providerType: ProviderType,
        baseUrl: String,
        directApiKey: String?,
        relayToken: String?
    ): SettingsValidationResult {
        val errors = mutableListOf<Pair<String, String>>()

        val baseUrlError = baseUrlValidationError(baseUrl)
        if (baseUrlError != null) {
            errors.add("baseUrl" to baseUrlError)
        }

        when (providerType) {
            ProviderType.DIRECT -> {
                if (directApiKey.isNullOrBlank()) {
                    errors.add("credential" to "Direct mode requires an API key")
                }
            }
            ProviderType.RELAY -> {
                if (relayToken.isNullOrBlank()) {
                    errors.add("credential" to "Relay mode requires your own backend relay token")
                }
            }
        }

        return if (errors.isEmpty() && baseUrl.isNotBlank()) {
            SettingsValidationResult.ready()
        } else {
            SettingsValidationResult.notReady(*errors.toTypedArray())
        }
    }
}

internal fun baseUrlValidationError(baseUrl: String): String? {
    if (baseUrl.isBlank()) {
        return null
    }

    val normalized = baseUrl.trim().removeSuffix("/")
    return if (normalized.endsWith("/chat/completions")) {
        "OpenAI Base URL must not include /chat/completions"
    } else {
        null
    }
}

internal fun normalizeChatCompletionsEndpoint(baseUrl: String): String {
    if (baseUrl.isBlank()) {
        return ""
    }

    baseUrlValidationError(baseUrl)?.let { error ->
        throw ChatError.Config(error)
    }

    return "${baseUrl.trim().removeSuffix("/")}/chat/completions"
}
