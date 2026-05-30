package com.neo.locallm.online

import android.content.Context

class OnlinePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("online_llm_prefs", Context.MODE_PRIVATE)

    var huggingFaceToken: String
        get() = prefs.getString(KEY_HUGGING_FACE_TOKEN, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_HUGGING_FACE_TOKEN, value.trim()).apply()
        }

    companion object {
        private const val KEY_HUGGING_FACE_TOKEN = "huggingface_token"
    }
}
