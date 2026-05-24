package com.neo.locallm.online

import android.content.Context

class OnlinePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("online_llm_prefs", Context.MODE_PRIVATE)

    var openRouterApiKey: String
        get() = prefs.getString(KEY_OPENROUTER_API_KEY, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_OPENROUTER_API_KEY, value.trim()).apply()
        }

    companion object {
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
    }
}
