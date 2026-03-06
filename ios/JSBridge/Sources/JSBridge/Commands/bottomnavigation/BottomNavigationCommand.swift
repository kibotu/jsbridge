import Foundation

public class BottomNavigationCommand: BridgeCommand {
    public let action = "bottomNavigation"

    weak var bridge: JavaScriptBridge?

    public init(bridge: JavaScriptBridge? = nil) {
        self.bridge = bridge
    }

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let isVisible = content?["isVisible"] as? Bool else {
            throw BridgeError.invalidParameter("isVisible")
        }

        BottomNavigationService.shared.setVisible(isVisible)
        SafeAreaService.shared.pushToBridge(bridge)
        return nil
    }
}

