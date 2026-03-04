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

    func handle(
        content: [String: Any]?,
        completion: @escaping (Result<[String: Any]?, BridgeError>) -> Void
    ) {
        DispatchQueue.main.async { [weak self] in
            guard let vc = self?.viewController else {
                completion(.failure(.internalError("No view controller")))
                return
            }

            let safeArea = vc.view.safeAreaInsets
            let statusBarHeight = vc.view.window?.windowScene?.statusBarManager?.statusBarFrame.height ?? safeArea.top

            let result: [String: Any] = [
                "statusBar": [
                    "height": Int(statusBarHeight),
                    "visible": !(vc.view.window?.windowScene?.statusBarManager?.isStatusBarHidden ?? false)
                ],
                "systemNavigation": [
                    "height": Int(safeArea.bottom),
                    "visible": safeArea.bottom > 0
                ],
                "keyboard": [
                    "height": 0,
                    "visible": false
                ],
                "safeArea": [
                    "top": Int(safeArea.top),
                    "right": Int(safeArea.right),
                    "bottom": Int(safeArea.bottom),
                    "left": Int(safeArea.left)
                ]
            ]

            completion(.success(result))
        }
    }
}
