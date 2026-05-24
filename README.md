
<h1 align="center">NEO Local LM</h1>

<p align="center">
<img src="art/logo.png"/>
</p>

NEO Local LM is a local-first chat app for Android, Windows, iOS, and Mac. Download GGUF models, load them in one tap, and chat privately with llama.cpp inference. If you choose an OpenRouter model, the app can switch cleanly between online and local mode.

![preview](art/Preview.png)

## Features

- **Local GGUF inference** - no cloud needed for local models
- **Rich markdown** in chat responses - headers, code blocks, lists, and more
- **Reasoning model support** - thinking steps from compatible models are displayed in a styled section
- **Reliable background downloads** - custom download engine with OkHttp and WorkManager, progress notifications with speed and ETA, automatic resume on network interruptions
- **Storage management** - choose where to keep multi-GB model files with Android's Storage Access Framework
- **Desktop downloads** - Windows app downloads models to a selected folder or the default Downloads folder
- **OpenRouter support** - online-only Nemotron models are available when an API key is saved
- **Apple target** - the SwiftUI project builds for iPhone, iPad, and Mac desktop through Mac Catalyst
- **ARM optimized** - KleidiAI kernels and OpenMP for faster generation on arm64 devices
- **Large-screen ready** - tablets, foldables, and Chromebooks get a permanent sessions sidebar, list-detail Settings, and freeform window resize support

## Supported Models

| Family | Sizes | Provider |
|--------|-------|----------|
| Qwen 3 | 1.7B default | Alibaba |
| DeepSeek R1 Distill | 1.5B | DeepSeek |
| Gemma 3 | 1B | Google |
| Gemma2 | 9B | Google |
| Gemma 3n | 4B | Google |
| TinyLlama | 1.1B Q5 | TinyLlama |
| LFM2.5 Thinking | 1.2B | Liquid AI |
| Ministral 3 | 8B Instruct, 8B Reasoning | Mistral |
| Llama 3.2 | 3B Instruct Uncensored | Meta/Llama community GGUF |
| Qwen2.5.1-Coder | 7B Instruct | Alibaba/community GGUF |
| OLMo 2 1124 | 7B Instruct | Allen AI/community GGUF |

Models are GGUF-format, usually Q4_K_M, Q5_K_M, or Q6_K quantization, sourced from [Hugging Face](https://huggingface.co/).

## Online Option

Settings includes an OpenRouter API key field. When configured, the model picker shows **Nemotron 3 Nano (OpenRouter)** and **Nemotron 3 Super 120B Free (OpenRouter)** as online-only options. Selecting one bypasses local llama.cpp loading until you unload the online model or load a local GGUF model.

## Install

Build locally from this repository. Android APKs are built with Gradle, Windows packages are built from `windows/build_windows.ps1`, and Apple builds are produced with Xcode on macOS.

## Build Instructions

Prerequisites:
* Android Studio [2024.3.1+](https://developer.android.com/studio/releases)
* NDK 27.2.12479018
* CMake 3.31.6

1. Open the project in Android Studio: `File` > `Open`.
2. Connect an Android device or start an emulator.
3. Run the application using `Run` > `Run 'app'` or the play button in Android Studio.
4. For command-line APK builds, run `./gradlew assembleRelease`.

Windows:

```powershell
.\windows\build_windows.ps1
```

iOS and Mac:

```sh
open ios/NEOLocalLM/NEOLocalLM.xcodeproj
```

Choose an iPhone/iPad destination for iOS, or `My Mac (Mac Catalyst)` for the Mac desktop build.

## License

This project is licensed under the [MIT License](LICENSE).

## Acknowledgments

This project is built on [llama.cpp](https://github.com/ggml-org/llama.cpp). Models are GGUF-format with Q4_K_M quantization sourced from [Hugging Face](https://huggingface.co/).

## Contact

OpenRouter keys are stored locally in app settings.
