package com.chatppp.app.data.local.secrets

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSecretStore(context: Context) : SecretStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getDirectApiKey(): String? = preferences.getString(KEY_DIRECT_API, null)

    override fun saveDirectApiKey(value: String) {
        preferences.edit().putString(KEY_DIRECT_API, value).apply()
    }

    override fun clearDirectApiKey() {
        preferences.edit().remove(KEY_DIRECT_API).apply()
    }

    override fun getRelayToken(): String? = preferences.getString(KEY_RELAY_TOKEN, null)

    override fun saveRelayToken(value: String) {
        preferences.edit().putString(KEY_RELAY_TOKEN, value).apply()
    }

    override fun clearRelayToken() {
        preferences.edit().remove(KEY_RELAY_TOKEN).apply()
    }

    override fun getPresetDirectApiKey(presetId: String): String? =
        preferences.getString("preset_direct_api_key_$presetId", null)

    override fun savePresetDirectApiKey(
        presetId: String,
        value: String
    ) {
        preferences.edit().putString("preset_direct_api_key_$presetId", value).apply()
    }

    override fun clearPresetDirectApiKey(presetId: String) {
        preferences.edit().remove("preset_direct_api_key_$presetId").apply()
    }

    override fun getPresetRelayToken(presetId: String): String? =
        preferences.getString("preset_relay_token_$presetId", null)

    override fun savePresetRelayToken(
        presetId: String,
        value: String
    ) {
        preferences.edit().putString("preset_relay_token_$presetId", value).apply()
    }

    override fun clearPresetRelayToken(presetId: String) {
        preferences.edit().remove("preset_relay_token_$presetId").apply()
    }

    private companion object {
        const val FILE_NAME = "chatppp_secrets"
        const val KEY_DIRECT_API = "direct_api_key"
        const val KEY_RELAY_TOKEN = "relay_token"
    }
}
