package com.neo.locallm.settings

import android.content.Context

class SecurityPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    var biometricPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_PIN_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BIOMETRIC_PIN_ENABLED, value).apply()
        }

    companion object {
        private const val KEY_BIOMETRIC_PIN_ENABLED = "biometric_pin_enabled"
    }
}
