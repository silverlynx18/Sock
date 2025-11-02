import SwiftUI

@available(iOS 17.0, *)
struct SockRootView: View {
    var state: SockAppState
    var onStateChange: (SockAppState) -> Void
    var isRailExpanded: Bool
    var toggleRail: () -> Void

    var body: some View {
        NavigationSplitView(
            sidebar: {
                NavigationRail(
                    destinations: SockDestination.allCases,
                    selected: .dashboard,
                    expanded: isRailExpanded,
                    toggleRail: toggleRail
                )
            },
            detail: {
                DashboardScreen(
                    state: state,
                    onUpdateGlobalStatus: cycleGlobalStatus,
                    onSelectGroup: { _ in },
                    onViewInvitations: {},
                    onManageGroups: {}
                )
                .navigationTitle("Sock")
            }
        )
    }

    private func cycleGlobalStatus() {
        let statuses: [AvailabilityStatus] = [.openToHangout, .busy, .working, .doNotApproach]
        guard let currentIndex = statuses.firstIndex(of: state.globalStatus) else { return }
        let nextIndex = (currentIndex + 1) % statuses.count
        var updated = state
        updated.globalStatus = statuses[nextIndex]
        onStateChange(updated)
    }
}

@available(iOS 17.0, *)
private struct NavigationRail: View {
    let destinations: [SockDestination]
    let selected: SockDestination
    let expanded: Bool
    let toggleRail: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Button(action: toggleRail) {
                Label("Toggle", systemImage: "sofa.fill")
                    .labelStyle(.iconOnly)
            }
            .buttonStyle(.borderless)

            ForEach(destinations) { destination in
                NavigationLink(value: destination) {
                    Label(destination.title, systemImage: destination.systemImage)
                        .labelStyle(expanded ? .titleAndIcon : .iconOnly)
                }
                .buttonStyle(.borderless)
            }
            Spacer()
        }
        .padding(.vertical, 24)
        .frame(minWidth: expanded ? 160 : 72)
        .background(.ultraThinMaterial)
    }
}

enum SockDestination: String, Identifiable, CaseIterable {
    case dashboard
    case manageGroups
    case settings
    case profile

    var id: String { rawValue }

    var title: String {
        switch self {
        case .dashboard: return "Dashboard"
        case .manageGroups: return "Groups"
        case .settings: return "Settings"
        case .profile: return "Profile"
        }
    }

    var systemImage: String {
        switch self {
        case .dashboard: return "rectangle.grid.2x2"
        case .manageGroups: return "person.3"
        case .settings: return "gearshape"
        case .profile: return "person.crop.circle"
        }
    }
}

@available(iOS 17.0, *)
struct SockRootView_Previews: PreviewProvider {
    static var previews: some View {
        SockRootView(
            state: SockPreviewState.make(),
            onStateChange: { _ in },
            isRailExpanded: true,
            toggleRail: {}
        )
    }
}
