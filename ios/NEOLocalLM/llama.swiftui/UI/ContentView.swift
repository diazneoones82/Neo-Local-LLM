import LocalAuthentication
import SwiftUI
import UIKit

struct ContentView: View {
    @EnvironmentObject private var state: NEOAppState
    @AppStorage("dark_mode_enabled") private var darkModeEnabled = false
    @AppStorage("onboarding_seen") private var onboardingSeen = false
    @State private var text = ""
    @State private var showingModels = false
    @State private var showingSettings = false
    @State private var showingClearConfirmation = false

    var body: some View {
        NavigationStack {
            ZStack {
                chatSurface
                    .sheet(isPresented: $showingModels) {
                        ModelPickerView()
                            .environmentObject(state)
                    }
                    .sheet(isPresented: $showingSettings) {
                        SettingsView()
                            .environmentObject(state)
                    }

                if !onboardingSeen {
                    OnboardingView {
                        onboardingSeen = true
                    }
                }
            }
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    EmptyView()
                }
                ToolbarItem(placement: .principal) {
                    Button {
                        showingModels = true
                    } label: {
                        VStack(spacing: 1) {
                            Text(state.modelTitle)
                                .font(.headline)
                                .lineLimit(1)
                            Text(state.modelSubtitle)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }
                    .buttonStyle(.plain)
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    if case .online = state.loadedMode {
                        Button {
                            state.unloadModel()
                        } label: {
                            Image(systemName: "eject")
                        }
                    }
                    Button {
                        showingClearConfirmation = true
                    } label: {
                        Image(systemName: "trash")
                    }
                    Button {
                        state.newConversation()
                    } label: {
                        Image(systemName: "square.and.pencil")
                    }
                    Button {
                        showingSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .alert("Clear chat?", isPresented: $showingClearConfirmation) {
                Button("Cancel", role: .cancel) { }
                Button("Clear", role: .destructive) {
                    state.clearChat()
                }
            } message: {
                Text("Clear the current chat window and start fresh with the loaded model?")
            }
        }
        .preferredColorScheme(darkModeEnabled ? .dark : .light)
    }

    private var chatSurface: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        if state.messages.isEmpty {
                            VStack(spacing: 12) {
                                Image(systemName: "sparkles")
                                    .font(.system(size: 44))
                                    .foregroundStyle(.tint)
                                Text("NEO Local LM")
                                    .font(.title.bold())
                                Text("Download Qwen 3 1.7B, load it locally, or select an OpenRouter model from the picker.")
                                    .font(.body)
                                    .foregroundStyle(.secondary)
                                    .multilineTextAlignment(.center)
                            }
                            .padding(.top, 80)
                        }
                        ForEach(state.messages) { message in
                            MessageBubble(message: message)
                                .id(message.id)
                        }
                    }
                    .padding()
                }
                .onChange(of: state.messages.count) { _ in
                    if let last = state.messages.last {
                        proxy.scrollTo(last.id, anchor: .bottom)
                    }
                }
            }
            Divider()
            HStack(spacing: 10) {
                TextField("Message", text: $text, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(1...5)
                Button {
                    let payload = text
                    text = ""
                    Task { await state.send(payload) }
                } label: {
                    Image(systemName: state.isGenerating ? "hourglass" : "paperplane.fill")
                }
                .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || state.isGenerating)
                .buttonStyle(.borderedProminent)
            }
            .padding()
        }
    }
}

private struct MessageBubble: View {
    let message: ChatMessage

    var body: some View {
        HStack {
            if message.role == .assistant {
                bubble
                Spacer(minLength: 40)
            } else {
                Spacer(minLength: 40)
                bubble
            }
        }
    }

    private var bubble: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(message.content.isEmpty ? "..." : message.content)
                .textSelection(.enabled)
            if message.role == .assistant && !message.content.isEmpty {
                Button {
                    UIPasteboard.general.string = message.content
                } label: {
                    Label("Copy", systemImage: "doc.on.doc")
                }
                .font(.caption)
                .buttonStyle(.borderless)
            }
        }
        .padding(12)
        .background(message.role == .user ? Color.accentColor.opacity(0.16) : Color.secondary.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct ModelPickerView: View {
    @EnvironmentObject private var state: NEOAppState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section("Local Models") {
                    ForEach(state.localModels) { model in
                        VStack(alignment: .leading, spacing: 8) {
                            Text(model.name)
                                .font(.headline)
                            Text(model.description)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            HStack {
                                if state.isDownloaded(model) {
                                    Button("Load") {
                                        Task {
                                            await state.loadLocalModel(model)
                                            dismiss()
                                        }
                                    }
                                    .buttonStyle(.borderedProminent)
                                    Button("Delete", role: .destructive) {
                                        state.delete(model)
                                    }
                                    .buttonStyle(.bordered)
                                } else {
                                    Button("Download") {
                                        Task { await state.download(model) }
                                    }
                                    .buttonStyle(.borderedProminent)
                                }
                                if let progress = state.downloadProgress[model.id] {
                                    Text(progress)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                        .padding(.vertical, 6)
                    }
                }
                Section("Online Only") {
                    ForEach(state.onlineModels) { model in
                        Button {
                            state.loadOnlineModel(model)
                            dismiss()
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(model.name)
                                Text(model.description)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Models")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

private struct SettingsView: View {
    @EnvironmentObject private var state: NEOAppState
    @Environment(\.dismiss) private var dismiss
    @AppStorage("dark_mode_enabled") private var darkModeEnabled = false
    @State private var apiKeyDraft = ""
    @State private var showingModels = false
    @State private var showingHistory = false

    var body: some View {
        NavigationStack {
            List {
                Toggle("Dark Mode", isOn: $darkModeEnabled)
                Picker("Language", selection: $state.selectedLanguage) {
                    Text("System").tag("System")
                    Text("English").tag("English")
                    Text("Hindi").tag("Hindi")
                    Text("Spanish").tag("Spanish")
                    Text("French").tag("French")
                }
                Section("OpenRouter API Key") {
                    SecureField("OpenRouter API Key", text: $apiKeyDraft)
                    Button("Save") {
                        state.saveOpenRouterKey(apiKeyDraft)
                    }
                }
                Button("Models") {
                    showingModels = true
                }
                Button("Conversation History") {
                    showingHistory = true
                }
                Toggle(
                    "Biometric/PIN",
                    isOn: Binding(
                        get: { state.biometricPinEnabled },
                        set: { state.setBiometricPinEnabled($0) }
                    )
                )
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .onAppear {
                apiKeyDraft = state.openRouterApiKey
            }
            .sheet(isPresented: $showingModels) {
                ModelPickerView()
                    .environmentObject(state)
            }
            .sheet(isPresented: $showingHistory) {
                ConversationHistoryView()
                    .environmentObject(state)
            }
        }
    }
}

private struct ConversationHistoryView: View {
    @EnvironmentObject private var state: NEOAppState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if state.sessions.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "clock")
                            .font(.largeTitle)
                            .foregroundStyle(.secondary)
                        Text("No conversations")
                            .font(.headline)
                        Text("Chats you start will appear here.")
                            .font(.body)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
                }
                ForEach(state.sessions) { session in
                    Button {
                        state.loadSession(session)
                        dismiss()
                    } label: {
                        VStack(alignment: .leading) {
                            Text(session.title)
                                .lineLimit(1)
                            Text(session.updatedAt, style: .date)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Conversation History")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

private struct OnboardingView: View {
    let onFinish: () -> Void
    @State private var page = 0

    private let slides = [
        ("Download a local model", "Open Settings, choose Models, then download Qwen 3 1.7B or another GGUF model.", "arrow.down.circle"),
        ("Load and chat locally", "Select a downloaded model from the top bar. Once loaded, messages are generated with llama.cpp on device.", "cpu"),
        ("Use online only when needed", "Add your OpenRouter key to use Nemotron online, or as fallback when local generation fails.", "cloud")
    ]

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()
            VStack(spacing: 24) {
                Image(systemName: slides[page].2)
                    .font(.system(size: 54))
                    .foregroundStyle(.tint)
                Text(slides[page].0)
                    .font(.largeTitle.bold())
                    .multilineTextAlignment(.center)
                Text(slides[page].1)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                HStack {
                    ForEach(slides.indices, id: \.self) { index in
                        Circle()
                            .fill(index == page ? Color.accentColor : Color.secondary.opacity(0.3))
                            .frame(width: 8, height: 8)
                    }
                }
                HStack {
                    Button("Skip") {
                        onFinish()
                    }
                    Spacer()
                    Button(page == slides.count - 1 ? "Get started" : "Next") {
                        if page == slides.count - 1 {
                            onFinish()
                        } else {
                            page += 1
                        }
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(.horizontal)
            }
            .padding(28)
        }
    }
}
