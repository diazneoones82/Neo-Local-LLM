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
            name = "LFM2 8B A1B",
            filename = "LFM2-8B-A1B-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/LiquidAI/LFM2-8B-A1B-GGUF/resolve/main/LFM2-8B-A1B-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-07-15"),
            description = "Default local downloadable model - Liquid AI - 8B A1B - Q4_K_M",
            logoRes = R.drawable.logo_liquid,
            supportedLanguages = LFM_LANGS
        ),
        ModelInfo(
            name = "Qwen 3 1.7B",
            filename = "Qwen3-1.7B-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/lmstudio-community/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2025-04-29"),
            description = "Local reasoning model - 1.28Gb - Q4_K_M",
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
            name = "NVIDIA Nemotron 3 Nano 4B",
            filename = "NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf",
            remoteUri = Uri.parse("https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF/resolve/main/NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf"),
            releaseDate = LocalDate.parse("2026-05-20"),
            description = "NVIDIA local instruct model - 4B - Q4_K_M",
            logoRes = R.drawable.logo_nvidia,
            supportedLanguages = ENGLISH_ONLY
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

    private fun huggingFaceModel(
        name: String,
        filename: String,
        logoRes: Int,
        modelId: String
    ): ModelInfo = ModelInfo(
        name = name,
        filename = "online-huggingface-$filename",
        remoteUri = null,
        releaseDate = null,
        description = "Hugging Face online model - token required",
        logoRes = logoRes,
        supportedLanguages = MULTILINGUAL_BROAD,
        isOnline = true,
        additionalFiles = listOf(ModelFilePart("huggingface-model-id", Uri.parse(modelId)))
    )

    private fun openRouterModel(
        name: String,
        filename: String,
        logoRes: Int,
        modelId: String
    ): ModelInfo = ModelInfo(
        name = name,
        filename = "online-openrouter-$filename",
        remoteUri = null,
        releaseDate = null,
        description = "OpenRouter online model - API key required",
        logoRes = logoRes,
        supportedLanguages = MULTILINGUAL_BROAD,
        isOnline = true,
        additionalFiles = listOf(ModelFilePart("openrouter-model-id", Uri.parse(modelId)))
    )

    val huggingFaceGemma4: ModelInfo = huggingFaceModel(
        name = "Gemma 4 31B IT (Hugging Face)",
        filename = "gemma-4-31b-it",
        logoRes = R.drawable.logo_google,
        modelId = "google/gemma-4-31B-it"
    )

    val huggingFaceDeepSeekV4Flash: ModelInfo = huggingFaceModel(
        name = "DeepSeek V4 Flash (Hugging Face)",
        filename = "deepseek-v4-flash",
        logoRes = R.drawable.logo_deepseek,
        modelId = "deepseek-ai/DeepSeek-V4-Flash"
    )

    val openRouterGemma426BFree: ModelInfo = openRouterModel(
        name = "Gemma 4 26B A4B IT (OpenRouter)",
        filename = "gemma-4-26b-a4b-it-free",
        logoRes = R.drawable.logo_google,
        modelId = "google/gemma-4-26b-a4b-it:free"
    )

    val openRouterLagunaM1Free: ModelInfo = openRouterModel(
        name = "Laguna M.1 (OpenRouter)",
        filename = "laguna-m-1-free",
        logoRes = R.drawable.penrose_triangle,
        modelId = "poolside/laguna-m.1:free"
    )

    val openRouterNemotron3SuperFree: ModelInfo = openRouterModel(
        name = "Nemotron 3 Super 120B A12B (OpenRouter)",
        filename = "nemotron-3-super-120b-a12b-free",
        logoRes = R.drawable.logo_nvidia,
        modelId = "nvidia/nemotron-3-super-120b-a12b:free"
    )

    val openRouterStep35FlashFree: ModelInfo = openRouterModel(
        name = "Step 3.5 Flash (OpenRouter)",
        filename = "step-3-5-flash-free",
        logoRes = R.drawable.penrose_triangle,
        modelId = "stepfun/step-3.5-flash:free"
    )

    val openRouterDeepSeekV4FlashFree: ModelInfo = openRouterModel(
        name = "DeepSeek V4 Flash (OpenRouter)",
        filename = "deepseek-v4-flash-free",
        logoRes = R.drawable.logo_deepseek,
        modelId = "deepseek/deepseek-v4-flash:free"
    )

    val onlineModels: List<ModelInfo> = listOf(
        huggingFaceGemma4,
        huggingFaceDeepSeekV4Flash,
        openRouterGemma426BFree,
        openRouterLagunaM1Free,
        openRouterNemotron3SuperFree,
        openRouterStep35FlashFree,
        openRouterDeepSeekV4FlashFree
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

    fun getHuggingFaceModelId(model: ModelInfo): String? =
        model.additionalFiles.firstOrNull { it.filename == "huggingface-model-id" }
            ?.remoteUri
            ?.toString()

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
                            .filterNot { it.filename == "huggingface-model-id" }
                            .all { it.filename in downloadedFilenames },
                )
            }
        val customWithStatus = customModels.map { model ->
            ModelWithStatus(model = model, isDownloaded = true)
        }
        return customWithStatus + knownModels
    }
}
