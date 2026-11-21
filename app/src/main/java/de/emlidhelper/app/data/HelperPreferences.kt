package de.emlidhelper.app.data

import android.content.Context
import androidx.core.content.edit

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

    companion object {
        private const val KEY_WAIT_SECONDS = "airplane_wait_seconds"
        private const val KEY_CLEAR_CACHE = "clear_cache"
        private const val KEY_FORCE_STOP = "force_stop_emlid"
        private const val KEY_AIRPLANE = "toggle_airplane_mode"
        private const val KEY_LAUNCH_AFTER = "launch_emlid_after"
    }
}
