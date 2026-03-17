package com.chatppp.app.di

import com.chatppp.app.data.context.ConversationSummaryStore
import com.chatppp.app.data.context.DefaultConversationSummaryStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.chatppp.app.data.local.db.ChatPppDatabase
import com.chatppp.app.data.local.db.ConversationDao
import com.chatppp.app.data.local.db.MessageDao
import com.chatppp.app.data.local.db.ConversationSummaryDao
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.preferences.PreferencesLastConversationStore
import com.chatppp.app.data.local.secrets.EncryptedSecretStore
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.domain.session.LastConversationStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ChatPppDatabase = Room.databaseBuilder(
        context,
        ChatPppDatabase::class.java,
        "chatppp.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideConversationDao(database: ChatPppDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: ChatPppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideConversationSummaryDao(database: ChatPppDatabase): ConversationSummaryDao =
        database.conversationSummaryDao()

    @Provides
    @Singleton
    fun provideConversationSummaryStore(
        dao: ConversationSummaryDao
    ): ConversationSummaryStore = DefaultConversationSummaryStore(dao)

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.filesDir.resolve("app-preferences.preferences_pb") }
    )

    @Provides
    @Singleton
    fun provideAppPreferences(
        dataStore: DataStore<Preferences>
    ): AppPreferences = AppPreferences(dataStore)

    @Provides
    @Singleton
    fun provideLastConversationStore(
        appPreferences: AppPreferences
    ): LastConversationStore = PreferencesLastConversationStore(appPreferences)

    @Provides
    @Singleton
    fun provideSecretStore(
        @ApplicationContext context: Context
    ): SecretStore = EncryptedSecretStore(context)
}
