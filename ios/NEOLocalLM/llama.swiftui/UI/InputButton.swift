import SwiftUI

struct InputButton: View {
    @EnvironmentObject private var state: NEOAppState
    @Binding var text: String

    var body: some View {
        Button {
            let payload = text
            text = ""
            Task { await state.send(payload) }
        } label: {
            Image(systemName: "paperplane.fill")
        }
        .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || state.isGenerating)
    }
}
