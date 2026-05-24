package com.diazneoones82.llamacpp.jni

import com.diazneoones82.llamacpp.LlamaGenerationCallback

class NativeLlamaSession {

    private var nativeHandle: Long = 0

    external fun generate(callback: LlamaGenerationCallback): Int

    external fun addMessage(message: String, enableThinking: Boolean)

    external fun printReport()

    external fun getReport(): String

    external fun replayHistory(userMessages: Array<String>, assistantMessages: Array<String>)

    external fun destroy()
}
