import SwiftUI

struct DownloadButton: View {
    @EnvironmentObject private var state: NEOAppState
    let model: LocalModel

    var body: some View {
        Button(state.isDownloaded(model) ? "Load" : "Download") {
            Task {
                if state.isDownloaded(model) {
                    await state.loadLocalModel(model)
                } else {
                    await state.download(model)
                }
            }
        }
    }
}
