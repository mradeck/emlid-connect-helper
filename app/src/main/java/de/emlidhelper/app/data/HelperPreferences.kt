package de.emlidhelper.app.data

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

class HelperPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("emlid_helper_prefs", Context.MODE_PRIVATE)

    fun loadResetConfig(): ResetConfig = ResetConfig(
        airplaneWaitSeconds = prefs.getInt(KEY_WAIT_SECONDS, 15).coerceIn(5, 60),
        clearCache = prefs.getBoolean(KEY_CLEAR_CACHE, true),
        forceStopEmlid = prefs.getBoolean(KEY_FORCE_STOP, true),
        toggleAirplaneMode = prefs.getBoolean(KEY_AIRPLANE, true),
        launchEmlidAfter = prefs.getBoolean(KEY_LAUNCH_AFTER, true)
    )

    fun saveResetConfig(config: ResetConfig) {
        prefs.edit {
            putInt(KEY_WAIT_SECONDS, config.airplaneWaitSeconds)
            putBoolean(KEY_CLEAR_CACHE, config.clearCache)
            putBoolean(KEY_FORCE_STOP, config.forceStopEmlid)
            putBoolean(KEY_AIRPLANE, config.toggleAirplaneMode)
            putBoolean(KEY_LAUNCH_AFTER, config.launchEmlidAfter)
        }
    }

    fun loadLanguage(): AppLanguage {
        val saved = prefs.getString(KEY_LANGUAGE, null)
        return when (saved) {
            AppLanguage.EN.name -> AppLanguage.EN
            AppLanguage.DE.name -> AppLanguage.DE
            else -> if (Locale.getDefault().language.equals("de", ignoreCase = true)) {
                AppLanguage.DE
            } else {
                AppLanguage.EN
            }
        }
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit { putString(KEY_LANGUAGE, language.name) }
    }

    fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, null)
        return runCatching { saved?.let { AppThemeMode.valueOf(it) } }.getOrNull()
            ?: AppThemeMode.SYSTEM
    }

    fun saveThemeMode(mode: AppThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    companion object {
        private const val KEY_WAIT_SECONDS = "airplane_wait_seconds"
        private const val KEY_CLEAR_CACHE = "clear_cache"
        private const val KEY_FORCE_STOP = "force_stop_emlid"
        private const val KEY_AIRPLANE = "toggle_airplane_mode"
        private const val KEY_LAUNCH_AFTER = "launch_emlid_after"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}
