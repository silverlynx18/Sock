import SwiftUI

@available(iOS 17.0, *)
struct DashboardScreen: View {
    let state: SockAppState
    let onUpdateGlobalStatus: () -> Void
    let onSelectGroup: (GroupSummary) -> Void
    let onViewInvitations: () -> Void
    let onManageGroups: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                GlobalStatusCard(
                    status: state.globalStatus,
                    onUpdate: onUpdateGlobalStatus
                )

                GroupActionsCard(onManageGroups: onManageGroups, onViewInvitations: onViewInvitations)

                if state.dashboardGroups.isEmpty {
                    EmptyState(onCreateGroup: onManageGroups)
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Groups")
                            .font(.title2.bold())
                        ForEach(state.dashboardGroups) { group in
                            GroupCard(group: group, onTap: { onSelectGroup(group) })
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(Color(.systemGroupedBackground))
    }
}

@available(iOS 17.0, *)
private struct GlobalStatusCard: View {
    let status: AvailabilityStatus
    let onUpdate: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Global Status", systemImage: "globe")
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(status.title)
                .font(.title2.weight(.semibold))
                .foregroundStyle(status.color)

            Text(status.description)
                .font(.body)

            Button("Update") {
                onUpdate()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

@available(iOS 17.0, *)
private struct GroupActionsCard: View {
    let onManageGroups: () -> Void
    let onViewInvitations: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Groups")
                .font(.title3.bold())
            Text("Create and manage your trusted circles.")
                .font(.callout)
                .foregroundStyle(.secondary)

            HStack {
                Button("Manage Groups", action: onManageGroups)
                Button("View Invitations", action: onViewInvitations)
            }
            .buttonStyle(.bordered)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 20))
    }
}

@available(iOS 17.0, *)
private struct GroupCard: View {
    let group: GroupSummary
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 16) {
                Circle()
                    .fill(group.status.color.gradient)
                    .frame(width: 36, height: 36)
                    .overlay(
                        Image(systemName: "person.3.fill")
                            .foregroundStyle(.white)
                    )

                VStack(alignment: .leading, spacing: 6) {
                    Text(group.name)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text("\(group.memberCount) members")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(group.status.title)
                        .font(.caption)
                        .bold()
                        .foregroundStyle(group.status.color)
                }

                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.secondarySystemBackground))
            )
        }
        .buttonStyle(.plain)
    }
}

@available(iOS 17.0, *)
private struct EmptyState: View {
    let onCreateGroup: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("No groups yet")
                .font(.title3.bold())
            Text("Create a group to start sharing availability with your circles.")
                .font(.body)
                .foregroundStyle(.secondary)
            Button("Create a group", action: onCreateGroup)
                .buttonStyle(.borderedProminent)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 20))
    }
}

@available(iOS 17.0, *)
#Preview("Dashboard") {
    DashboardScreen(
        state: SockPreviewState.make(),
        onUpdateGlobalStatus: {},
        onSelectGroup: { _ in },
        onViewInvitations: {},
        onManageGroups: {}
    )
    .frame(width: 400)
}
