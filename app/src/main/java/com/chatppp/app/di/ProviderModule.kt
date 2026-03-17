package com.chatppp.app.di

import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.remote.parser.ChatStreamParser
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.data.remote.provider.RuntimeProviderSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {
    @Provides
    @Singleton
    fun provideProviderSelector(
        okHttpClient: OkHttpClient,
        json: Json,
        streamParser: ChatStreamParser,
        appPreferences: AppPreferences,
        secretStore: SecretStore
    ): ProviderSelector = RuntimeProviderSelector(
        okHttpClient = okHttpClient,
        json = json,
        streamParser = streamParser,
        appPreferences = appPreferences,
        secretStore = secretStore
    )
}
