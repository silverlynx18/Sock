# Sock iOS Module

SwiftUI scaffolding for the Sock application. Mirrors the Android Compose architecture with local-first state so cross-platform features stay in sync.

## Structure
- `Package.swift` ? Swift Package definition (iOS 17 minimum).
- `Sources/SockApp/` ? SwiftUI scene, state models, and dashboard layout.
- `Tests/` ? Quick preview-state tests that run via `swift test`.

To run in Xcode:
1. `open Package.swift` or add the package to a workspace.
2. Set the `SockApplication` target as the app entry point (iOS 17 simulator).

## Next Steps
- Wire Room/DataStore equivalents (e.g., Core Data or SwiftData) behind repository protocols.
- Mirror feature work from Android/Web: groups, invitations, settings, profile.
- Introduce platform-specific assets and App Icons once design tokens are finalized.
