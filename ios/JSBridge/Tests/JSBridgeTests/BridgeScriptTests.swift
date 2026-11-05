import XCTest
@testable import JSBridge

private struct FakeCommand: BridgeCommand {
    let action: String
    var features: Set<String>

    init(_ action: String, features: Set<String>? = nil) {
        self.action = action
        self.features = features ?? [action]
    }

    @MainActor
    func handle(content: [String: Any]?) async throws -> [String: Any]? { nil }
}

final class BridgeScriptTests: XCTestCase {

    func testCommandContributesItsActionByDefault() {
        XCTAssertEqual(BridgeScript.featureNames([FakeCommand("haptic")]), ["haptic"])
    }

    func testDeclaredSubFeaturesAreAddedSortedAndUnique() {
        let commands: [any BridgeCommand] = [
            FakeCommand("haptic", features: ["haptic", "haptic.pattern"]),
            FakeCommand("deviceInfo"),
            FakeCommand("haptic"),
        ]

        XCTAssertEqual(BridgeScript.featureNames(commands), ["deviceInfo", "haptic", "haptic.pattern"])
    }

    func testNoCommandsMeansNoFeatures() {
        XCTAssertEqual(BridgeScript.featureNames([]), [])
    }

    func testTemplatePlaceholdersAreReplaced() {
        let script = BridgeScript.render("name=__BRIDGE_NAME__ f=__FEATURES__", name: "bridge", commands: [FakeCommand("toast")])

        XCTAssertEqual(script, "name=bridge f=[\"toast\"]")
    }

    func testLegacyVersionFieldIsIgnoredWhenDecoding() throws {
        let json = #"{"version": 7, "id": "1", "data": {"action": "ping"}}"#.data(using: .utf8)!

        let message = try JSONDecoder().decode(JavaScriptBridgeMessage.self, from: json)

        XCTAssertEqual(message.data.action, "ping")
    }
}
