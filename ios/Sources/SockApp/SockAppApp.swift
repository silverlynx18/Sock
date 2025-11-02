import SwiftUI

@available(iOS 17.0, *)
public struct SockApplication: App {
    @State private var appState = SockPreviewState.make()
    @State private var isRailExpanded = false

    public init() {}

    public var body: some Scene {
        WindowGroup {
            SockRootView(
                state: appState,
                onStateChange: { appState = $0 },
                isRailExpanded: isRailExpanded,
                toggleRail: { isRailExpanded.toggle() }
            )
        }
    }
}
