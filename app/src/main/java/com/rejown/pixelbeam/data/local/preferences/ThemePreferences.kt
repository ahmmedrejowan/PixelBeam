package com.rejown.pixelbeam.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelbeam_preferences")

class ThemePreferences(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_preference")
    private val dynamicColorKey = stringPreferencesKey("dynamic_color_preference")

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = theme
        }
    }

    fun getTheme(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[themeModeKey] ?: "System"
        }
    }

    suspend fun saveDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dynamicColorKey] = if (enabled) "true" else "false"
        }
    }

    fun isDynamicColorEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[dynamicColorKey]?.toBoolean() ?: false
        }
    }
}