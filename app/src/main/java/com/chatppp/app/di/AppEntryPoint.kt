package com.chatppp.app.di

import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun chatRepository(): ChatRepository

    fun lastConversationStore(): LastConversationStore

    fun appPreferences(): AppPreferences

    fun secretStore(): SecretStore

    fun configPresetStore(): ConfigPresetStore
}
