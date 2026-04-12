package com.chatppp.app.di

import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.remote.provider.ConnectionTestService
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun chatRepository(): ChatRepository

    fun lastConversationStore(): LastConversationStore

    fun appPreferences(): AppPreferences

    fun secretStore(): SecretStore

    fun configPresetStore(): ConfigPresetStore

    fun providerSelector(): ProviderSelector

    fun okHttpClient(): OkHttpClient

    fun json(): Json

    fun connectionTestService(): ConnectionTestService
}
