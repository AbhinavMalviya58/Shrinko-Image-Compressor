package com.cheatpot.shrinko.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cheatpot.shrinko.domain.model.AppSettings
import com.cheatpot.shrinko.domain.model.CompressionMode
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixel_shrink_settings")

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val formatKey = stringPreferencesKey("default_format")
    private val modeKey = stringPreferencesKey("default_mode")
    private val qualityKey = intPreferencesKey("default_quality")
    private val resizeKey = stringPreferencesKey("default_resize")

    val appSettingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            defaultOutputFormat = runCatching { OutputFormat.valueOf(prefs[formatKey] ?: OutputFormat.JPEG.name) }.getOrDefault(OutputFormat.JPEG),
            defaultCompressionMode = runCatching { CompressionMode.valueOf(prefs[modeKey] ?: CompressionMode.AUTO.name) }.getOrDefault(CompressionMode.AUTO),
            defaultQuality = prefs[qualityKey] ?: 75,
            defaultResizeMode = runCatching { ResizeMode.valueOf(prefs[resizeKey] ?: ResizeMode.NONE.name) }.getOrDefault(ResizeMode.NONE)
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeKey] = settings.themeMode.name
            prefs[formatKey] = settings.defaultOutputFormat.name
            prefs[modeKey] = settings.defaultCompressionMode.name
            prefs[qualityKey] = settings.defaultQuality
            prefs[resizeKey] = settings.defaultResizeMode.name
        }
    }
}
