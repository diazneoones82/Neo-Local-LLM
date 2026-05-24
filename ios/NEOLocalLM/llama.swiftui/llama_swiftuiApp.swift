import SwiftUI

@main
struct NEOLocalLMApp: App {
    @StateObject private var state = NEOAppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(state)
        }
    }
}
