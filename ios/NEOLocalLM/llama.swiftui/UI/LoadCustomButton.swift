import SwiftUI
import UniformTypeIdentifiers

struct LoadCustomButton: View {
    @EnvironmentObject private var state: NEOAppState
    @State private var showFileImporter = false

    var body: some View {
        Button("Load Custom GGUF") {
            showFileImporter = true
        }
        .fileImporter(
            isPresented: $showFileImporter,
            allowedContentTypes: [UTType(filenameExtension: "gguf", conformingTo: .data)!],
            allowsMultipleSelection: false
        ) { result in
            guard case .success(let urls) = result,
                  let source = urls.first else { return }
            let gotAccess = source.startAccessingSecurityScopedResource()
            defer {
                if gotAccess { source.stopAccessingSecurityScopedResource() }
            }
            Task { await state.loadCustomModel(at: source) }
        }
    }
}
