package com.neo.locallm.online

import com.neo.locallm.BuildConfig
import com.neo.locallm.conversation.Message
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OnlineFallbackClient(
    private val onlinePreferences: OnlinePreferences? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generate(systemPrompt: String, messages: List<Message>): String? {
        return tryGemini(systemPrompt, messages) ?: tryOpenAi(systemPrompt, messages)
    }

    suspend fun generateOpenRouterNemotron(systemPrompt: String, messages: List<Message>): String? {
        return tryOpenRouter(systemPrompt, messages, OPENROUTER_NEMOTRON_MODEL)
    }

    suspend fun generateOpenRouterModel(
        systemPrompt: String,
        messages: List<Message>,
        modelId: String
    ): String? {
        return tryOpenRouter(systemPrompt, messages, modelId)
    }

    private fun tryOpenRouter(
        systemPrompt: String,
        messages: List<Message>,
        model: String
    ): String? {
        val apiKey = onlinePreferences?.openRouterApiKey.orEmpty()
        if (apiKey.isBlank()) return null

        val body = JSONObject()
            .put("model", model)
            .put("messages", openAiCompatibleMessages(systemPrompt, messages))

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://neolocallm.local")
            .addHeader("X-OpenRouter-Title", "NEO Local LM")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        return executeTextRequest(request) { json ->
            json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
        }
    }

    private fun tryGemini(systemPrompt: String, messages: List<Message>): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return null

        val body = JSONObject().apply {
            if (systemPrompt.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt))
                    )
                )
            }
            put(
                "contents",
                JSONArray().apply {
                    messages.forEach { message ->
                        put(
                            JSONObject()
                                .put("role", if (message.author == "User") "user" else "model")
                                .put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", message.content))
                                )
                        )
                    }
                }
            )
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_FALLBACK_MODEL}:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        return executeTextRequest(request) { json ->
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.joinTextParts()
        }
    }

    private fun tryOpenAi(systemPrompt: String, messages: List<Message>): String? {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) return null

        val input = openAiCompatibleMessages(systemPrompt, messages)
        val body = JSONObject()
            .put("model", BuildConfig.OPENAI_FALLBACK_MODEL)
            .put("input", input)

        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        return executeTextRequest(request) { json ->
            json.optString("output_text").takeIf { it.isNotBlank() }
                ?: json.optJSONArray("output")?.extractOpenAiText()
        }
    }

    private fun openAiCompatibleMessages(systemPrompt: String, messages: List<Message>): JSONArray {
        return JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
            }
            messages.forEach { message ->
                put(
                    JSONObject()
                        .put("role", if (message.author == "User") "user" else "assistant")
                        .put("content", message.content)
                )
            }
        }
    }

    private fun executeTextRequest(
        request: Request,
        parser: (JSONObject) -> String?
    ): String? {
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                parser(JSONObject(body))?.trim()?.takeIf { it.isNotBlank() }
            }
        } catch (_: IOException) {
            null
        } catch (_: org.json.JSONException) {
            null
        }
    }

    private fun JSONArray.joinTextParts(): String {
        return buildString {
            for (i in 0 until length()) {
                val text = optJSONObject(i)?.optString("text").orEmpty()
                if (text.isNotBlank()) append(text)
            }
        }
    }

    private fun JSONArray.extractOpenAiText(): String {
        val builder = StringBuilder()
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                val text = part.optString("text")
                if (text.isNotBlank()) builder.append(text)
            }
        }
        return builder.toString()
    }

    companion object {
        const val OPENROUTER_NEMOTRON_MODEL = "nvidia/nemotron-3-nano-30b-a3b"
    }
}
