# NEO Local LM for iOS and Mac

SwiftUI Apple app scaffold matching the Android and Windows NEO Local LM experience. The same Xcode target builds for iPhone, iPad, and Mac desktop through Mac Catalyst.

- llama.cpp local GGUF loading through the bundled Swift bridge
- Qwen 3 1.7B as the default local model
- curated local model downloads, including split GGUF support for Qwen3-Coder-Next
- OpenRouter online-only options for Nemotron 3 Nano and Nemotron 3 Super 120B Free
- OpenRouter API key persistence
- first-run onboarding slides
- settings sequence: Dark Mode, Language, OpenRouter API key, Models, Conversation History, Biometric/PIN
- chat history, clear chat, copy assistant response, online unload

## Build on macOS

1. Build the llama.cpp Apple framework from the repository root:

   ```sh
   cd app/src/main/cpp/llama.cpp
   ./build-xcframework.sh
   ```

   The Xcode project expects:

   ```text
   app/src/main/cpp/llama.cpp/build-apple/llama.xcframework
   ```

2. Open:

   ```text
   ios/NEOLocalLM/NEOLocalLM.xcodeproj
   ```

3. Select the `NEO Local LM` target.

4. For iOS, choose an iPhone or iPad destination.

5. For Mac desktop, choose `My Mac (Mac Catalyst)`.

6. Build/run with Xcode, or from macOS terminal:

   ```sh
   xcodebuild -project ios/NEOLocalLM/NEOLocalLM.xcodeproj -scheme "NEO Local LM" -configuration Release -destination 'generic/platform=iOS' build
   ```

   Mac Catalyst desktop build:

   ```sh
   xcodebuild -project ios/NEOLocalLM/NEOLocalLM.xcodeproj -scheme "NEO Local LM" -configuration Release -destination 'platform=macOS,variant=Mac Catalyst' build
   ```

An `.ipa`, signed `.app`, or notarized Mac package requires Apple signing credentials and must be produced on macOS with Xcode.
