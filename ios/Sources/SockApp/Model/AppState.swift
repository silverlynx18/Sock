import Foundation
import SwiftUI

public struct AvailabilityStatus: Identifiable, Equatable, Hashable {
    public let id: String
    public let title: String
    public let description: String
    public let color: Color

    public init(id: String, title: String, description: String, color: Color) {
        self.id = id
        self.title = title
        self.description = description
        self.color = color
    }

    public static let openToHangout = AvailabilityStatus(
        id: "open_to_hangout",
        title: "Open to Hangout",
        description: "I'm free and would love to connect.",
        color: .green
    )

    public static let busy = AvailabilityStatus(
        id: "busy",
        title: "Busy",
        description: "Unavailable for now; feel free to reach out later.",
        color: .orange
    )

    public static let working = AvailabilityStatus(
        id: "working",
        title: "Working",
        description: "Focused on work; ping if it's important.",
        color: .yellow
    )

    public static let doNotApproach = AvailabilityStatus(
        id: "do_not_approach",
        title: "Do Not Approach",
        description: "Need serious space right now.",
        color: .red
    )
}

public struct GroupSummary: Identifiable, Equatable {
    public let id: UUID
    public let name: String
    public let memberCount: Int
    public let primaryColor: Color
    public let secondaryColor: Color
    public let status: AvailabilityStatus

    public init(
        id: UUID,
        name: String,
        memberCount: Int,
        primaryColor: Color,
        secondaryColor: Color,
        status: AvailabilityStatus
    ) {
        self.id = id
        self.name = name
        self.memberCount = memberCount
        self.primaryColor = primaryColor
        self.secondaryColor = secondaryColor
        self.status = status
    }
}

public struct InvitationSummary: Identifiable, Equatable {
    public let id: UUID
    public let groupName: String
    public let inviterName: String
    public let sentAt: Date
}

public struct CustomStatus: Identifiable, Equatable {
    public enum Tone: String, CaseIterable {
        case relaxed, social, focused, boundary

        public var color: Color {
            switch self {
            case .relaxed: return .green
            case .social: return .blue
            case .focused: return .orange
            case .boundary: return .red
            }
        }

        public var label: String {
            switch self {
            case .relaxed: return "Relaxed"
            case .social: return "Social"
            case .focused: return "Focused"
            case .boundary: return "Boundary"
            }
        }
    }

    public let id: UUID
    public let text: String
    public let tone: Tone
    public let note: String?
}

public struct SockAppState {
    public var globalStatus: AvailabilityStatus
    public var managedGroups: [GroupSummary]
    public var memberGroups: [GroupSummary]
    public var invitations: [InvitationSummary]
    public var customStatuses: [CustomStatus]

    public init(
        globalStatus: AvailabilityStatus,
        managedGroups: [GroupSummary],
        memberGroups: [GroupSummary],
        invitations: [InvitationSummary],
        customStatuses: [CustomStatus]
    ) {
        self.globalStatus = globalStatus
        self.managedGroups = managedGroups
        self.memberGroups = memberGroups
        self.invitations = invitations
        self.customStatuses = customStatuses
    }

    public var dashboardGroups: [GroupSummary] {
        managedGroups + memberGroups
    }
}

enum SockPreviewState {
    static func make() -> SockAppState {
        let roommates = GroupSummary(
            id: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!,
            name: "Roommates",
            memberCount: 4,
            primaryColor: Color(red: 0.4, green: 0.3, blue: 0.9),
            secondaryColor: Color(red: 0.2, green: 0.1, blue: 0.4),
            status: .openToHangout
        )

        let bookClub = GroupSummary(
            id: UUID(uuidString: "00000000-0000-0000-0000-000000000002")!,
            name: "Book Club",
            memberCount: 8,
            primaryColor: Color(red: 0.2, green: 0.6, blue: 0.3),
            secondaryColor: Color(red: 0.1, green: 0.3, blue: 0.15),
            status: .working
        )

        return SockAppState(
            globalStatus: .openToHangout,
            managedGroups: [roommates],
            memberGroups: [bookClub],
            invitations: [
                InvitationSummary(
                    id: UUID(uuidString: "00000000-0000-0000-0000-000000000010")!,
                    groupName: "Game Night",
                    inviterName: "Jordan",
                    sentAt: Date()
                )
            ],
            customStatuses: [
                CustomStatus(
                    id: UUID(),
                    text: "Take a breather",
                    tone: .relaxed,
                    note: "Happy for a quick walk"
                ),
                CustomStatus(
                    id: UUID(),
                    text: "Heads down sprint",
                    tone: .focused,
                    note: nil
                )
            ]
        )
    }
}
