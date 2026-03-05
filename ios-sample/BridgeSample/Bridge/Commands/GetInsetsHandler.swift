import Foundation
import UIKit

/// Returns current system inset values so web content can adapt its layout.
///
/// Web usage:
/// ```javascript
/// const insets = await jsbridge.call({ data: { action: 'getInsets' } });
/// ```
class GetInsetsHandler: BridgeCommand {
    let actionName = "getInsets"

    weak var viewController: UIViewController?

    init(viewController: UIViewController?) {
        self.viewController = viewController
    }

    @MainActor
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let vc = viewController else {
            throw BridgeError.internalError("No view controller")
        }

        let windowScene = vc.view.window?.windowScene
            ?? UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first

        let statusBarHeight = windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let isStatusBarHidden = windowScene?.statusBarManager?.isStatusBarHidden ?? false
        let rootSafeArea = vc.view.window?.safeAreaInsets ?? vc.view.safeAreaInsets

        return [
            "statusBar": [
                "height": Int(statusBarHeight),
                "visible": !isStatusBarHidden
            ],
            "systemNavigation": [
                "height": Int(rootSafeArea.bottom),
                "visible": rootSafeArea.bottom > 0
            ],
            "keyboard": [
                "height": 0,
                "visible": false
            ],
            "safeArea": [
                "top": Int(rootSafeArea.top),
                "right": Int(rootSafeArea.right),
                "bottom": Int(rootSafeArea.bottom),
                "left": Int(rootSafeArea.left)
            ]
        ]
    }
}
