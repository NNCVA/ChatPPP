package com.chatppp.app.data.local.preferences

import kotlinx.coroutines.flow.Flow

interface ConfigPresetPreferencesStore {
    val configPresetMetadata: Flow<List<ConfigPresetMetadata>>

    val activePresetId: Flow<String?>

    suspend fun updateConfigPresetMetadata(
        transform: (List<ConfigPresetMetadata>) -> List<ConfigPresetMetadata>
    )

    suspend fun setActivePresetId(presetId: String?)
}
