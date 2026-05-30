import Foundation
import SwiftUI

struct ModelPart: Codable, Hashable {
    let filename: String
    let url: URL
}

struct LocalModel: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let filename: String
    let url: URL
    let additionalFiles: [ModelPart]
    let description: String
    let isDefault: Bool
}

struct OnlineModel: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let modelId: String
    let provider: String
    let description: String
}

struct ChatMessage: Identifiable, Codable, Hashable {
    let id: UUID
    let role: Role
    var content: String
    let createdAt: Date

    enum Role: String, Codable {
        case user
        case assistant
    }
}

struct ChatSession: Identifiable, Codable, Hashable {
    let id: UUID
    var title: String
    var messages: [ChatMessage]
    var updatedAt: Date
}

enum ModelMode: Equatable {
    case none
    case local(LocalModel)
    case online(OnlineModel)
}

@MainActor
final class NEOAppState: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var sessions: [ChatSession] = []
    @Published var loadedMode: ModelMode = .none
    @Published var isGenerating = false
    @Published var downloadProgress: [String: String] = [:]
    @Published var selectedLanguage = "System"
    @Published var huggingFaceToken = UserDefaults.standard.string(forKey: Keys.huggingFaceToken) ?? ""
    @Published var biometricPinEnabled = UserDefaults.standard.bool(forKey: Keys.biometricPinEnabled)

    let localModels: [LocalModel] = ModelCatalog.localModels
    let onlineModels: [OnlineModel] = ModelCatalog.onlineModels

    private var llamaContext: LlamaContext?
    private let sessionStoreURL: URL

    init() {
        sessionStoreURL = Self.documentsDirectory.appendingPathComponent("neo-chat-sessions.json")
        loadSessions()
    }

    var modelTitle: String {
        switch loadedMode {
        case .none:
            return "Select Model"
        case .local(let model):
            return model.name
        case .online(let model):
            return model.name
        }
    }

    var modelSubtitle: String {
        switch loadedMode {
        case .none:
            return "Local or Hugging Face"
        case .local(let model):
            return model.description
        case .online(let model):
            return model.description
        }
    }

    func saveHuggingFaceToken(_ value: String) {
        huggingFaceToken = value.trimmingCharacters(in: .whitespacesAndNewlines)
        UserDefaults.standard.set(huggingFaceToken, forKey: Keys.huggingFaceToken)
    }

    func setBiometricPinEnabled(_ enabled: Bool) {
        biometricPinEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: Keys.biometricPinEnabled)
    }

    func isDownloaded(_ model: LocalModel) -> Bool {
        ([model.filename] + model.additionalFiles.map(\.filename)).allSatisfy {
            FileManager.default.fileExists(atPath: modelFileURL($0).path)
        }
    }

    func download(_ model: LocalModel) async {
        let parts = [ModelPart(filename: model.filename, url: model.url)] + model.additionalFiles
        for (index, part) in parts.enumerated() {
            if FileManager.default.fileExists(atPath: modelFileURL(part.filename).path) {
                continue
            }
            downloadProgress[model.id] = "Downloading \(index + 1) of \(parts.count)"
            do {
                let (temporaryURL, _) = try await URLSession.shared.download(from: part.url)
                let destination = modelFileURL(part.filename)
                try? FileManager.default.removeItem(at: destination)
                try FileManager.default.moveItem(at: temporaryURL, to: destination)
            } catch {
                downloadProgress[model.id] = "Download failed"
                return
            }
        }
        downloadProgress[model.id] = nil
    }

    func delete(_ model: LocalModel) {
        ([model.filename] + model.additionalFiles.map(\.filename)).forEach {
            try? FileManager.default.removeItem(at: modelFileURL($0))
        }
        if case .local(let loaded) = loadedMode, loaded.id == model.id {
            unloadModel()
        }
    }

    func loadLocalModel(_ model: LocalModel) async {
        guard isDownloaded(model) else {
            await download(model)
            if !isDownloaded(model) { return }
        }
        do {
            llamaContext = try LlamaContext.create_context(path: modelFileURL(model.filename).path)
            loadedMode = .local(model)
        } catch {
            llamaContext = nil
            loadedMode = .none
        }
    }

    func loadCustomModel(at url: URL) async {
        do {
            llamaContext = try LlamaContext.create_context(path: url.path)
            loadedMode = .local(
                LocalModel(
                    id: url.lastPathComponent,
                    name: url.deletingPathExtension().lastPathComponent,
                    filename: url.lastPathComponent,
                    url: url,
                    additionalFiles: [],
                    description: "Custom local GGUF model",
                    isDefault: false
                )
            )
        } catch {
            llamaContext = nil
            loadedMode = .none
        }
    }

    func loadOnlineModel(_ model: OnlineModel) {
        llamaContext = nil
        loadedMode = .online(model)
    }

    func unloadModel() {
        llamaContext = nil
        loadedMode = .none
    }

    func clearChat() {
        messages.removeAll()
    }

    func newConversation() {
        persistCurrentSession()
        messages.removeAll()
    }

    func loadSession(_ session: ChatSession) {
        messages = session.messages
    }

    func send(_ text: String) async {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !isGenerating else { return }

        messages.append(ChatMessage(id: UUID(), role: .user, content: trimmed, createdAt: Date()))
        let assistantId = UUID()
        messages.append(ChatMessage(id: assistantId, role: .assistant, content: "", createdAt: Date()))
        isGenerating = true

        let response: String?
        switch loadedMode {
        case .local:
            response = await generateLocal(prompt: trimmed)
                ?? await generateOnline(model: ModelCatalog.fallbackOnlineModel, prompt: trimmed)
        case .online(let model):
            response = await generateOnline(model: model, prompt: trimmed)
        case .none:
            response = "Download and load a local model, or select a Hugging Face online model in the model picker."
        }

        if let index = messages.firstIndex(where: { $0.id == assistantId }) {
            messages[index].content = response?.isEmpty == false ? response! : "No response was returned."
        }
        isGenerating = false
        persistCurrentSession()
    }

    private func generateLocal(prompt: String) async -> String? {
        guard let llamaContext else { return nil }
        await llamaContext.completion_init(text: prompt)
        var output = ""
        while await !llamaContext.is_done {
            output += await llamaContext.completion_loop()
        }
        await llamaContext.clear()
        return output.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : output
    }

    private func generateOnline(model: OnlineModel, prompt: String) async -> String? {
        let key = huggingFaceToken.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else { return nil }

        let endpoint = "https://router.huggingface.co/v1/chat/completions"
        var request = URLRequest(url: URL(string: endpoint)!)
        request.httpMethod = "POST"
        request.setValue("Bearer \(key)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let history = messages
            .filter { !$0.content.isEmpty }
            .map { ["role": $0.role == .user ? "user" : "assistant", "content": $0.content] }
        var body: [String: Any] = [
            "model": model.modelId,
            "messages": history.isEmpty ? [["role": "user", "content": prompt]] : history
        ]
        body["stream"] = false
        body["max_tokens"] = 2048
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }
            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            let choices = json?["choices"] as? [[String: Any]]
            let message = choices?.first?["message"] as? [String: Any]
            return message?["content"] as? String
        } catch {
            return nil
        }
    }

    private func persistCurrentSession() {
        guard !messages.isEmpty else { return }
        let title = messages.first(where: { $0.role == .user })?.content.prefix(48) ?? "Conversation"
        let session = ChatSession(id: UUID(), title: String(title), messages: messages, updatedAt: Date())
        sessions.removeAll { $0.messages == messages }
        sessions.insert(session, at: 0)
        sessions = Array(sessions.prefix(50))
        saveSessions()
    }

    private func loadSessions() {
        guard let data = try? Data(contentsOf: sessionStoreURL),
              let decoded = try? JSONDecoder().decode([ChatSession].self, from: data) else {
            return
        }
        sessions = decoded
    }

    private func saveSessions() {
        guard let data = try? JSONEncoder().encode(sessions) else { return }
        try? data.write(to: sessionStoreURL, options: [.atomic])
    }

    private func modelFileURL(_ filename: String) -> URL {
        Self.documentsDirectory.appendingPathComponent(filename)
    }

    private static var documentsDirectory: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    private enum Keys {
        static let huggingFaceToken = "huggingface_token"
        static let biometricPinEnabled = "biometric_pin_enabled"
    }
}

enum ModelCatalog {
    static let localModels: [LocalModel] = [
        LocalModel(
            id: "lfm2-8b-a1b",
            name: "LFM2 8B A1B",
            filename: "LFM2-8B-A1B-Q4_K_M.gguf",
            url: URL(string: "https://huggingface.co/LiquidAI/LFM2-8B-A1B-GGUF/resolve/main/LFM2-8B-A1B-Q4_K_M.gguf")!,
            additionalFiles: [],
            description: "Default local downloadable model - Liquid AI - 8B A1B - Q4_K_M",
            isDefault: true
        ),
        LocalModel(
            id: "qwen3-1-7b",
            name: "Qwen 3 1.7B",
            filename: "Qwen3-1.7B-Q4_K_M.gguf",
            url: URL(string: "https://huggingface.co/lmstudio-community/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf")!,
            additionalFiles: [],
            description: "Local reasoning model - 1.28Gb - Q4_K_M",
            isDefault: false
        ),
        LocalModel(id: "deepseek-r1-distill", name: "DeepSeek R1 Distill", filename: "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/lmstudio-community/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf")!, additionalFiles: [], description: "DeepSeek reasoning model - 1.12Gb - Q4_K_M", isDefault: false),
        LocalModel(id: "gemma-3-1b", name: "Gemma 3 1B", filename: "gemma-3-1b-it-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/lmstudio-community/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf")!, additionalFiles: [], description: "Google lightweight chat model - 806Mb - Q4_K_M", isDefault: false),
        LocalModel(id: "lfm25-thinking", name: "LFM2.5 1.2B Thinking", filename: "LFM2.5-1.2B-Thinking-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/lmstudio-community/LFM2.5-1.2B-Thinking-GGUF/resolve/main/LFM2.5-1.2B-Thinking-Q4_K_M.gguf")!, additionalFiles: [], description: "Liquid AI thinking model - 731Mb - Q4_K_M", isDefault: false),
        LocalModel(id: "lfm25-thinking-f16", name: "LFM2.5 1.2B Thinking F16", filename: "LFM2.5-1.2B-Thinking-F16.gguf", url: URL(string: "https://huggingface.co/LiquidAI/LFM2.5-1.2B-Thinking-GGUF/resolve/main/LFM2.5-1.2B-Thinking-F16.gguf")!, additionalFiles: [], description: "Liquid AI full precision thinking model - F16", isDefault: false),
        LocalModel(id: "ministral-3-8b-instruct", name: "Ministral 3 8B Instruct", filename: "Ministral-3-8B-Instruct-2512-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/lmstudio-community/Ministral-3-8B-Instruct-2512-GGUF/resolve/main/Ministral-3-8B-Instruct-2512-Q4_K_M.gguf")!, additionalFiles: [], description: "Mistral instruct model - 5.2Gb - Q4_K_M", isDefault: false),
        LocalModel(id: "ministral-3-8b-reasoning", name: "Ministral 3 8B Reasoning", filename: "Ministral-3-8B-Reasoning-2512-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/lmstudio-community/Ministral-3-8B-Reasoning-2512-GGUF/resolve/main/Ministral-3-8B-Reasoning-2512-Q4_K_M.gguf")!, additionalFiles: [], description: "Mistral reasoning model - 5.2Gb - Q4_K_M", isDefault: false),
        LocalModel(id: "gemma-3n-4b", name: "Gemma 3n 4B", filename: "gemma-3n-E4B-it-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/lmstudio-community/gemma-3n-E4B-it-text-GGUF/resolve/main/gemma-3n-E4B-it-Q4_K_M.gguf")!, additionalFiles: [], description: "Google efficient on-device model - 4.24Gb - Q4_K_M", isDefault: false),
        LocalModel(id: "nvidia-nemotron3-nano-4b", name: "NVIDIA Nemotron 3 Nano 4B", filename: "NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF/resolve/main/NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf")!, additionalFiles: [], description: "NVIDIA local instruct model - 4B - Q4_K_M", isDefault: false),
        LocalModel(id: "gemma2-9b", name: "Gemma2 9B", filename: "gemma-2-9b-it-Q4_K_M.gguf", url: URL(string: "https://huggingface.co/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf")!, additionalFiles: [], description: "Google advanced chat model - 5.44Gb - Q4_K_M", isDefault: false),
        LocalModel(id: "tinyllama-q5", name: "TinyLlama 1.1B Q5", filename: "tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf", url: URL(string: "https://huggingface.co/pbatra/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf")!, additionalFiles: [], description: "TinyLlama lightweight chat model - Q5_K_M", isDefault: false),
        LocalModel(id: "llama-3-2-3b-uncensored", name: "Llama 3.2 3B Instruct Uncensored", filename: "Llama-3.2-3B-Instruct-uncensored-Q8_0.gguf", url: URL(string: "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-uncensored-GGUF/resolve/main/Llama-3.2-3B-Instruct-uncensored-Q8_0.gguf")!, additionalFiles: [], description: "Llama local instruct model - 3B - Q8_0", isDefault: false),
        LocalModel(id: "qwen251-coder-7b-instruct", name: "Qwen2.5.1-Coder 7B Instruct", filename: "Qwen2.5.1-Coder-7B-Instruct-Q6_K_L.gguf", url: URL(string: "https://huggingface.co/bartowski/Qwen2.5.1-Coder-7B-Instruct-GGUF/resolve/main/Qwen2.5.1-Coder-7B-Instruct-Q6_K_L.gguf")!, additionalFiles: [], description: "Qwen coding instruct model - 7B - Q6_K_L", isDefault: false),
        LocalModel(id: "olmo-2-1124-7b-instruct", name: "OLMo 2 1124 7B Instruct", filename: "OLMo-2-1124-7B-Instruct-Q6_K.gguf", url: URL(string: "https://huggingface.co/bartowski/OLMo-2-1124-7B-Instruct-GGUF/resolve/main/OLMo-2-1124-7B-Instruct-Q6_K.gguf")!, additionalFiles: [], description: "OLMo instruct model - 7B - Q6_K", isDefault: false)
    ]

    static let onlineModels: [OnlineModel] = [
        OnlineModel(id: "hf-gemma-4-31b-it", name: "Gemma 4 31B IT (Hugging Face)", modelId: "google/gemma-4-31B-it", provider: "huggingface", description: "Hugging Face online model - token required"),
        OnlineModel(id: "hf-deepseek-v4-flash", name: "DeepSeek V4 Flash (Hugging Face)", modelId: "deepseek-ai/DeepSeek-V4-Flash", provider: "huggingface", description: "Hugging Face online model - token required")
    ]

    static let fallbackOnlineModel = onlineModels[0]
}
