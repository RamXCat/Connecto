package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stitch_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_LOCK_ENABLED = "pin_lock_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_SHUTDOWN_ALERTS_ENABLED = "shutdown_alerts_enabled"
        private const val KEY_CONNECTION_LOST_ALERTS_ENABLED = "connection_lost_alerts_enabled"
    }

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_LOCK_ENABLED, value).apply()

    var pinCode: String
        get() = prefs.getString(KEY_PIN_CODE, "1111") ?: "1111" // Default simple pin code
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var isShutdownAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHUTDOWN_ALERTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHUTDOWN_ALERTS_ENABLED, value).apply()

    var isConnectionLostAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CONNECTION_LOST_ALERTS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CONNECTION_LOST_ALERTS_ENABLED, value).apply()

    var volumeLevel: Int
        get() = prefs.getInt("volume_level", 50)
        set(value) = prefs.edit().putInt("volume_level", value).apply()

    var brightnessLevel: Int
        get() = prefs.getInt("brightness_level", 50)
        set(value) = prefs.edit().putInt("brightness_level", value).apply()
}
