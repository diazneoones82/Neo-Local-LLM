package com.neo.locallm

import android.app.Application
import android.content.ComponentName
import com.diazneoones82.llamacpp.InferenceClient
import com.diazneoones82.llamacpp.LlamaCpp
import com.neo.locallm.data.AppDatabase
import com.neo.locallm.data.ChatRepository
import com.neo.locallm.data.SystemPromptRepository
import com.neo.locallm.download.DownloadNotificationManager
import com.neo.locallm.inference.LlamaService
import com.neo.locallm.inference.ProcessUtils

class App : Application() {

    lateinit var inferenceClient: InferenceClient
        private set
    lateinit var llamaCpp: LlamaCpp
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var systemPromptRepository: SystemPromptRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // App.onCreate runs in *every* process the app spawns. The :llama
        // process only needs to host LlamaService — skip Room, repos, and
        // the inference-client binding (no service to bind from there).
        if (ProcessUtils.isLlamaProcess()) return

        inferenceClient = InferenceClient(
            appContext = applicationContext,
            serviceComponent = ComponentName(this, LlamaService::class.java),
        )
        inferenceClient.bind()
        llamaCpp = LlamaCpp(inferenceClient)

        DownloadNotificationManager.createChannel(this)
        val database = AppDatabase.getInstance(this)
        chatRepository = ChatRepository(database.chatDao())
        systemPromptRepository = SystemPromptRepository(database.systemPromptDao())
    }
}
