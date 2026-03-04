import Foundation

class BottomNavigationHandler: BridgeCommand {
    let actionName = "bottomNavigation"

    weak var bridge: JavaScriptBridge?

    init(bridge: JavaScriptBridge? = nil) {
        self.bridge = bridge
    }

    func handle(
        content: [String: Any]?,
        completion: @escaping (Result<[String: Any]?, BridgeError>) -> Void
    ) {
        guard let isVisible = content?["isVisible"] as? Bool else {
            completion(.failure(.invalidParameter("isVisible")))
            return
        }

        DispatchQueue.main.async { [weak self] in
            BottomNavigationService.shared.setVisible(isVisible)

            SafeAreaService.shared.pushToBridge(self?.bridge)

            completion(.success(nil))
        }
    }
}

