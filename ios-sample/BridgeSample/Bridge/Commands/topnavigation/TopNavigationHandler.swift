import Foundation

class TopNavigationHandler: BridgeCommand {
    let actionName = "topNavigation"

    weak var bridge: JavaScriptBridge?

    init(bridge: JavaScriptBridge? = nil) {
        self.bridge = bridge
    }

    func handle(
        content: [String: Any]?,
        completion: @escaping (Result<[String: Any]?, BridgeError>) -> Void
    ) {
        guard let content = content else {
            completion(.failure(.invalidParameter("content")))
            return
        }

        DispatchQueue.main.async { [weak self] in
            TopNavigationService.shared.update(
                isVisible: content["isVisible"] as? Bool,
                title: content["title"] as? String,
                showBackButton: content["showUpArrow"] as? Bool,
                showDivider: content["showDivider"] as? Bool,
                showLogo: content["showLogo"] as? Bool,
                showProfileIconWidget: content["showProfileIconWidget"] as? Bool
            )

            SafeAreaService.shared.pushToBridge(self?.bridge)

            completion(.success(nil))
        }
    }
}

