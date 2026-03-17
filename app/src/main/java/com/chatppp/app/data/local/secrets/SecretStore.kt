package com.chatppp.app.data.local.secrets

interface SecretStore {
    fun getDirectApiKey(): String?

    fun saveDirectApiKey(value: String)

    fun clearDirectApiKey()

    fun getRelayToken(): String?

    fun saveRelayToken(value: String)

    fun clearRelayToken()

    fun getPresetDirectApiKey(presetId: String): String? = null

    fun savePresetDirectApiKey(
        presetId: String,
        value: String
    ) = Unit

    fun clearPresetDirectApiKey(presetId: String) = Unit

    fun getPresetRelayToken(presetId: String): String? = null

    fun savePresetRelayToken(
        presetId: String,
        value: String
    ) = Unit

    fun clearPresetRelayToken(presetId: String) = Unit
}
