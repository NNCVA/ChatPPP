package com.chatppp.app.di

import com.chatppp.app.data.context.ConversationSummaryStore
import com.chatppp.app.data.local.db.ConversationDao
import com.chatppp.app.data.local.db.MessageDao
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.presets.DefaultConfigPresetStore
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.data.repository.DefaultChatRepository
import com.chatppp.app.domain.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideConfigPresetStore(
        appPreferences: AppPreferences,
        secretStore: SecretStore
    ): ConfigPresetStore = DefaultConfigPresetStore(
        appPreferences = appPreferences,
        secretStore = secretStore
    )

    @Provides
    @Singleton
    fun provideChatRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        appPreferences: AppPreferences,
        providerSelector: ProviderSelector,
        configPresetStore: ConfigPresetStore,
        conversationSummaryStore: ConversationSummaryStore
    ): ChatRepository = DefaultChatRepository(
        conversationDao = conversationDao,
        messageDao = messageDao,
        appPreferences = appPreferences,
        providerSelector = providerSelector,
        configPresetStore = configPresetStore,
        conversationSummaryStore = conversationSummaryStore
    )
}
