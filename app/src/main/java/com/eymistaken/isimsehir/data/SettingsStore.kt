package com.eymistaken.isimsehir.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eymistaken.isimsehir.model.AccentColor
import com.eymistaken.isimsehir.model.DEFAULT_CATEGORIES
import com.eymistaken.isimsehir.model.TimerEndVibration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("isim_sehir_settings")

/** Kalıcı ayarlar. Turlar kasıtlı olarak burada tutulmaz — oturumluk. */
data class Settings(
    val categories: List<String> = DEFAULT_CATEGORIES,
    val accent: AccentColor = AccentColor.Amber,
    val floatingTimer: Boolean = true,
    val durationSeconds: Int = 60,
    val haptics: Boolean = true,
    val timerEndVibration: TimerEndVibration = TimerEndVibration.Medium,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val CATEGORIES = stringPreferencesKey("categories")
        val ACCENT = stringPreferencesKey("accent")
        val FLOATING_TIMER = booleanPreferencesKey("floating_timer")
        val DURATION = intPreferencesKey("duration_seconds")
        val HAPTICS = booleanPreferencesKey("haptics")
        val TIMER_END_VIBRATION = stringPreferencesKey("timer_end_vibration")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            categories = prefs[Keys.CATEGORIES]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: DEFAULT_CATEGORIES,
            accent = AccentColor.fromKey(prefs[Keys.ACCENT]),
            floatingTimer = prefs[Keys.FLOATING_TIMER] ?: true,
            durationSeconds = prefs[Keys.DURATION] ?: 60,
            haptics = prefs[Keys.HAPTICS] ?: true,
            timerEndVibration = TimerEndVibration.fromKey(prefs[Keys.TIMER_END_VIBRATION]),
        )
    }

    suspend fun setCategories(categories: List<String>) {
        context.dataStore.edit { it[Keys.CATEGORIES] = categories.joinToString(SEPARATOR) }
    }

    suspend fun setAccent(accent: AccentColor) {
        context.dataStore.edit { it[Keys.ACCENT] = accent.key }
    }

    suspend fun setFloatingTimer(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_TIMER] = enabled }
    }

    suspend fun setDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.DURATION] = seconds }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setTimerEndVibration(value: TimerEndVibration) {
        context.dataStore.edit { it[Keys.TIMER_END_VIBRATION] = value.key }
    }

    private companion object {
        /**
         * Kategoriler tek bir string'de saklanıyor. Ayraç olarak U+001F (unit
         * separator) seçildi; kullanıcının kategori adına yazamayacağı bir karakter.
         */
        const val SEPARATOR = "\u001F"
    }
}
