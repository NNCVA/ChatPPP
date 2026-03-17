package com.chatppp.app.data.presets

import com.chatppp.app.domain.model.ConfigPreset
import kotlinx.coroutines.flow.Flow

interface ConfigPresetStore {
    fun observePresets(): Flow<List<ConfigPreset>>

    val activePresetId: Flow<String?>

    suspend fun getPreset(presetId: String): ConfigPreset?

    suspend fun savePreset(preset: ConfigPreset)

    suspend fun deletePreset(presetId: String)

    suspend fun setActivePresetId(presetId: String?)
}

object NoOpConfigPresetStore : ConfigPresetStore {
    override fun observePresets(): Flow<List<ConfigPreset>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override val activePresetId: Flow<String?> = kotlinx.coroutines.flow.flowOf(null)

    override suspend fun getPreset(presetId: String): ConfigPreset? = null

    override suspend fun savePreset(preset: ConfigPreset) = Unit

    override suspend fun deletePreset(presetId: String) = Unit

    override suspend fun setActivePresetId(presetId: String?) = Unit
}
