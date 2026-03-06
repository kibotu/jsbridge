import Foundation

public class TopNavigationCommand: BridgeCommand {
    public let action = "topNavigation"

    weak var bridge: JavaScriptBridge?

    public init(bridge: JavaScriptBridge? = nil) {
        self.bridge = bridge
    }

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let content = content else {
            throw BridgeError.invalidParameter("content")
        }

        TopNavigationService.shared.update(
            isVisible: content["isVisible"] as? Bool,
            title: content["title"] as? String,
            showBackButton: content["showUpArrow"] as? Bool,
            showDivider: content["showDivider"] as? Bool,
            showLogo: content["showLogo"] as? Bool,
            showProfileIconWidget: content["showProfileIconWidget"] as? Bool
        )

        SafeAreaService.shared.pushToBridge(bridge)
        return nil
    }
}

