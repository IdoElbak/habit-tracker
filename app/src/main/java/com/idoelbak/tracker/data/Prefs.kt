package com.idoelbak.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val paletteId: String = DEFAULT_PALETTE_ID,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY
) {
    companion object {
        const val DEFAULT_PALETTE_ID = "indigo_sage"
    }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The handful of choices that change how the whole app behaves.
 *
 * Deliberately small: anything that can be derived is not stored, and the week start lives here
 * rather than in the repository because every date question already routes through one place.
 */
class Prefs(private val context: Context) {

    private val paletteKey = stringPreferencesKey("palette")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val weekStartKey = intPreferencesKey("week_start")

    val flow: Flow<Settings> = context.settingsStore.data
        // A corrupt or unreadable file must not take the app down; defaults are always usable.
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { stored ->
            Settings(
                paletteId = stored[paletteKey] ?: Settings.DEFAULT_PALETTE_ID,
                themeMode = stored[themeKey]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                weekStart = stored[weekStartKey]
                    ?.takeIf { it in 1..7 }
                    ?.let(DayOfWeek::of)
                    ?: DayOfWeek.SUNDAY
            )
        }

    suspend fun setPalette(id: String) = edit { it[paletteKey] = id }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[themeKey] = mode.name }

    suspend fun setWeekStart(day: DayOfWeek) = edit { it[weekStartKey] = day.value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit(block)
    }
}
