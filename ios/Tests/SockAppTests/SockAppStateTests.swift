import XCTest
@testable import SockApp

final class SockAppStateTests: XCTestCase {
    func testPreviewStateHasExpectedGroups() {
        let state = SockPreviewState.make()

        XCTAssertEqual(state.managedGroups.count, 1)
        XCTAssertEqual(state.memberGroups.count, 1)
        XCTAssertEqual(state.dashboardGroups.count, 2)
    }

    func testGlobalStatusCycles() {
        var state = SockPreviewState.make()
        let initial = state.globalStatus

        let statuses: [AvailabilityStatus] = [.openToHangout, .busy, .working, .doNotApproach]

        if let index = statuses.firstIndex(of: initial) {
            state.globalStatus = statuses[(index + 1) % statuses.count]
            XCTAssertNotEqual(initial, state.globalStatus)
        }
    }
}
