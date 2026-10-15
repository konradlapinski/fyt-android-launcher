package com.konradlapinski.fytlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore(name = "launcher_config")

class LauncherConfigStore(context: Context) {
    private val dataStore = context.launcherDataStore
    private val configKey = stringPreferencesKey("config_json")

    val config: Flow<LauncherConfig> = dataStore.data
        .map { preferences ->
            preferences[configKey]?.let { LauncherConfig.fromJson(it) } ?: LauncherConfig()
        }
        .catch { emit(LauncherConfig()) }

    suspend fun save(config: LauncherConfig) {
        dataStore.edit { preferences ->
            preferences[configKey] = config.normalized().toJson()
        }
    }
}
