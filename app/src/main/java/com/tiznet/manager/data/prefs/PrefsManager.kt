package com.tiznet.manager.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/** Lightweight preference store for the active panel and UI preferences. */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tiznet_prefs", Context.MODE_PRIVATE)

    var activePanelId: Long
        get() = prefs.getLong(KEY_ACTIVE_PANEL, -1L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_PANEL, value).apply()

    val hasActivePanel: Boolean
        get() = prefs.contains(KEY_ACTIVE_PANEL) && prefs.getLong(KEY_ACTIVE_PANEL, -1L) > 0L

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()

    var showAdvancedSettings: Boolean
        get() = prefs.getBoolean(KEY_ADVANCED, false)
        set(value) = prefs.edit().putBoolean(KEY_ADVANCED, value).apply()

    fun clearActivePanel() = prefs.edit().remove(KEY_ACTIVE_PANEL).apply()

    companion object {
        private const val KEY_ACTIVE_PANEL = "active_panel_id"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_ADVANCED = "advanced_settings"
    }
}
