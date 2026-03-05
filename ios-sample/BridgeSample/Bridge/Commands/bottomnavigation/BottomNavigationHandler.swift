import Foundation

class BottomNavigationHandler: BridgeCommand {
    let actionName = "bottomNavigation"

    weak var bridge: JavaScriptBridge?

    init(bridge: JavaScriptBridge? = nil) {
        self.bridge = bridge
    }

    @MainActor
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let isVisible = content?["isVisible"] as? Bool else {
            throw BridgeError.invalidParameter("isVisible")
        }

        BottomNavigationService.shared.setVisible(isVisible)
        SafeAreaService.shared.pushToBridge(bridge)
        return nil
    }
}

