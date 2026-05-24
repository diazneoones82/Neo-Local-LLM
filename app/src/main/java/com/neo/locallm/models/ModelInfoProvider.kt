package com.neo.locallm.models

import android.net.Uri
import com.neo.locallm.R
import java.time.LocalDate

object ModelInfoProvider {

    // Officially declared language support per model family (ISO 639-1 codes),
    // sourced from each model's HuggingFace card / publisher blog.
    private val MULTILINGUAL_BROAD = listOf(
        "en", "ar", "bg", "cs", "da", "de", "el", "es", "fi", "fr",
        "hi", "hu", "id", "it", "ja", "ko", "ms", "nl", "no", "pl",
        "pt", "ro", "ru", "sv", "th", "tr", "uk", "vi", "zh"
    )
    private val QWEN25_LANGS = listOf(
        "en", "zh", "fr", "es", "pt", "de", "it", "ru",
        "ja", "ko", "vi", "th", "ar"
    )
    private val LLAMA_LANGS = listOf("en", "de", "fr", "it", "pt", "hi", "es", "th")
    private val PHI_LANGS = listOf(
        "ar", "zh", "cs", "da", "nl", "en", "fi", "fr", "de", "he",
        "hu", "it", "ja", "ko", "no", "pl", "pt", "ru", "es", "sv",
        "th", "tr", "uk"
    )
    private val DEEPSEEK_LANGS = listOf("en", "zh")
    private val LFM_LANGS = listOf("en", "ar", "zh", "fr", "de", "ja", "ko", "es")
    private val MISTRAL_LANGS = listOf(
        "en", "fr", "de", "es", "it", "pt", "nl", "zh", "ja", "ko", "ar"
    )
    private val ENGLISH_ONLY = listOf("en")

    /**
     * Static list of all available models
     */
    val allModels: List<ModelInfo> = listOf(
        ModelInfo(
            name = "Qwen 3 1.7B",
            filename = "Qwen3-1.7B-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-04-29"),
            description = "Default local reasoning model - 1.28Gb - Q4_K_M",
            logoRes = R.drawable.logo_qwen,
            supportedLanguages = MULTILINGUAL_BROAD
        ),
        ModelInfo(
            name = "DeepSeek R1 Distill",
            filename = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-01-20"),
            description = "DeepSeek reasoning model - 1.12Gb - Q4_K_M",
            logoRes = R.drawable.logo_deepseek,
            supportedLanguages = DEEPSEEK_LANGS
        ),
        ModelInfo(
            name = "Gemma 3 1B",
            filename = "gemma-3-1b-it-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-03-12"),
            description = "Google Â· Lightweight chat model Â· 806Mb Â· Q4_K_M",
            logoRes = R.drawable.logo_google,
            supportedLanguages = MULTILINGUAL_BROAD
        ),
        ModelInfo(
            name = "LFM2.5 1.2B Thinking",
            filename = "LFM2.5-1.2B-Thinking-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/LFM2.5-1.2B-Thinking-GGUF/resolve/main/LFM2.5-1.2B-Thinking-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-01-09"),
            description = "Liquid AI Â· Thinking model Â· 731Mb Â· Q4_K_M",
            logoRes = R.drawable.logo_liquid,
            supportedLanguages = LFM_LANGS
        ),
        ModelInfo(
            name = "LFM2.5 1.2B Thinking F16",
            filename = "LFM2.5-1.2B-Thinking-F16.gguf",
            remoteUri = Uri.parse("https://huggingface.co/LiquidAI/LFM2.5-1.2B-Thinking-GGUF/resolve/main/LFM2.5-1.2B-Thinking-F16.gguf"),
            releaseDate = LocalDate.parse("2025-01-09"),
            description = "Liquid AI - full precision thinking model - F16",
            logoRes = R.drawable.logo_liquid,
            supportedLanguages = LFM_LANGS
        ),
        ModelInfo(
            name = "Ministral 3 8B Instruct",
            filename = "Ministral-3-8B-Instruct-2512-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/Ministral-3-8B-Instruct-2512-GGUF/resolve/main/Ministral-3-8B-Instruct-2512-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2024-12-17"),
            description = "Mistral Â· Instruct model Â· 5.2Gb Â· Q4_K_M",
            logoRes = R.drawable.logo_mistral,
            supportedLanguages = MISTRAL_LANGS
        ),
        ModelInfo(
            name = "Ministral 3 8B Reasoning",
            filename = "Ministral-3-8B-Reasoning-2512-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/Ministral-3-8B-Reasoning-2512-GGUF/resolve/main/Ministral-3-8B-Reasoning-2512-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2024-12-17"),
            description = "Mistral Â· Reasoning model Â· 5.2Gb Â· Q4_K_M",
            logoRes = R.drawable.logo_mistral,
            supportedLanguages = MISTRAL_LANGS
        ),
        ModelInfo(
            name = "Gemma 3n 4B",
            filename = "gemma-3n-E4B-it-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/gemma-3n-E4B-it-text-GGUF/resolve/main/gemma-3n-E4B-it-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-05-14"),
            description = "Google Â· Efficient on-device model Â· 4.24Gb Â· Q4_K_M",
            logoRes = R.drawable.logo_google,
            supportedLanguages = MULTILINGUAL_BROAD
        ),
        ModelInfo(
            name = "Gemma2 9B",
            filename = "gemma-2-9b-it-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2024-06-27"),
            description = "Google - advanced chat model - 5.44Gb - Q4_K_M",
            logoRes = R.drawable.logo_google,
            supportedLanguages = ENGLISH_ONLY
        ),
        ModelInfo(
            name = "TinyLlama 1.1B Q5",
            filename = "tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/pbatra/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf"),
            releaseDate = LocalDate.parse("2023-12-18"),
            description = "TinyLlama - lightweight chat model - Q5_K_M",
            logoRes = R.drawable.penrose_triangle,
            supportedLanguages = ENGLISH_ONLY
        ),
        ModelInfo(
            name = "Llama 3.2 3B Instruct Uncensored",
            filename = "Llama-3.2-3B-Instruct-uncensored-Q8_0.gguf",
            remoteUri = Uri.parse("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-uncensored-GGUF/resolve/main/Llama-3.2-3B-Instruct-uncensored-Q8_0.gguf"),
            releaseDate = LocalDate.parse("2024-09-25"),
            description = "Llama local instruct model - 3B - Q8_0",
            logoRes = R.drawable.penrose_triangle,
            supportedLanguages = LLAMA_LANGS
        ),
        ModelInfo(
            name = "Qwen2.5.1-Coder 7B Instruct",
            filename = "Qwen2.5.1-Coder-7B-Instruct-Q6_K_L.gguf",
            remoteUri = Uri.parse("https://huggingface.co/bartowski/Qwen2.5.1-Coder-7B-Instruct-GGUF/resolve/main/Qwen2.5.1-Coder-7B-Instruct-Q6_K_L.gguf"),
            releaseDate = LocalDate.parse("2025-01-28"),
            description = "Qwen coding instruct model - 7B - Q6_K_L",
            logoRes = R.drawable.logo_qwen,
            supportedLanguages = QWEN25_LANGS
        ),
        ModelInfo(
            name = "OLMo 2 1124 7B Instruct",
            filename = "OLMo-2-1124-7B-Instruct-Q6_K.gguf",
            remoteUri = Uri.parse("https://huggingface.co/bartowski/OLMo-2-1124-7B-Instruct-GGUF/resolve/main/OLMo-2-1124-7B-Instruct-Q6_K.gguf"),
            releaseDate = LocalDate.parse("2024-11-24"),
            description = "OLMo instruct model - 7B - Q6_K",
            logoRes = R.drawable.penrose_triangle,
            supportedLanguages = ENGLISH_ONLY
        )
    )

    private fun openRouterModel(
        name: String,
        filename: String,
        provider: String,
        logoRes: Int,
        modelId: String
    ): ModelInfo = ModelInfo(
        name = name,
        filename = "online-openrouter-$filename",
        remoteUri = null,
        releaseDate = null,
        description = "$provider online model - OpenRouter API key required",
        logoRes = logoRes,
        supportedLanguages = MULTILINGUAL_BROAD,
        isOnline = true,
        additionalFiles = listOf(ModelFilePart("openrouter-model-id", Uri.parse(modelId)))
    )

    val openRouterNemotron: ModelInfo = openRouterModel(
        name = "Nemotron 3 Nano (OpenRouter)",
        filename = "nemotron-3-nano",
        provider = "NVIDIA",
        logoRes = R.drawable.logo_nvidia,
        modelId = "nvidia/nemotron-3-nano-30b-a3b"
    )

    val openRouterNemotronSuper: ModelInfo = openRouterModel(
        name = "Nemotron 3 Super 120B Free (OpenRouter)",
        filename = "nemotron-3-super-120b-free",
        provider = "NVIDIA",
        logoRes = R.drawable.logo_nvidia,
        modelId = "nvidia/nemotron-3-super-120b-a12b:free"
    )

    val onlineModels: List<ModelInfo> = listOf(
        openRouterNemotron,
        openRouterNemotronSuper
    )

    val defaultModel: ModelInfo = allModels.first()
    
    /**
     * Get all known model filenames
     */
    val knownFilenames: Set<String> = allModels
        .flatMap { model -> listOf(model.filename) + model.additionalFiles.map { it.filename } }
        .toSet()
    
    /**
     * Get model by filename
     */
    fun getByFilename(filename: String): ModelInfo? =
        allModels.find { it.filename == filename } ?: onlineModels.find { it.filename == filename }
    
    /**
     * Get display name for a filename
     */
    fun getDisplayName(filename: String): String = getByFilename(filename)?.name ?: filename.removeSuffix(".gguf")

    fun getOpenRouterModelId(model: ModelInfo): String? =
        model.additionalFiles.firstOrNull { it.filename == "openrouter-model-id" }
            ?.remoteUri
            ?.toString()
    
    private fun formatFileSize(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 1.0) "%.2fGb".format(gb) else "%dMb".format(bytes / 1_000_000)
    }

    /**
     * Create a ModelInfo for a custom (user-provided) GGUF file.
     */
    fun createCustomModelInfo(filename: String, name: String, sizeBytes: Long): ModelInfo {
        val sizeLabel = formatFileSize(sizeBytes)
        return ModelInfo(
            name = name.ifEmpty { filename.removeSuffix(".gguf") },
            filename = filename,
            remoteUri = null,
            releaseDate = null,
            description = "Custom model Â· $sizeLabel",
            logoRes = R.drawable.penrose_triangle
        )
    }

    /**
     * Get models with their download status.
     */
    fun getModelsWithStatus(
        downloadedFilenames: Set<String>,
        customModels: List<ModelInfo> = emptyList()
    ): List<ModelWithStatus> {
        val knownModels = allModels
            .map { model ->
                ModelWithStatus(
                    model = model,
                    isDownloaded = model.filename in downloadedFilenames &&
                        model.additionalFiles
                            .filterNot { it.filename == "openrouter-model-id" }
                            .all { it.filename in downloadedFilenames },
                )
            }
        val customWithStatus = customModels.map { model ->
            ModelWithStatus(model = model, isDownloaded = true)
        }
        return customWithStatus + knownModels
    }
}
