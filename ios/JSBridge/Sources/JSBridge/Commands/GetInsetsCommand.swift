import Foundation
import UIKit

/// Returns current system inset values so web content can adapt its layout.
///
/// All heights are in points (pt) which map 1:1 to dp on Android, ensuring
/// cross-platform parity. `safeArea` includes both system bars and native
/// app chrome (top/bottom navigation) when visible.
///
/// Falls back from the view controller's window scene to the first connected scene
/// to handle edge cases where the VC's window is not yet in the hierarchy.
///
/// `@unchecked Sendable` because the weak `bridge` ref is only accessed
/// on `@MainActor`.
public final class GetInsetsCommand: BridgeCommand, BridgeAware, @unchecked Sendable {
    public let action = "insets"

    public weak var bridge: JavaScriptBridge?

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let vc = bridge?.viewController else {
            throw BridgeError.internalError("No view controller")
        }

        let windowScene = vc.view.window?.windowScene
            ?? UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first

        let statusBarHeight = windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let isStatusBarHidden = windowScene?.statusBarManager?.isStatusBarHidden ?? false
        let rootSafeArea = vc.view.window?.safeAreaInsets ?? vc.view.safeAreaInsets

        let isTopNavVisible = TopNavigationService.shared.config.isVisible
        let isBottomNavVisible = BottomNavigationService.shared.config.isVisible

        let topNavHeight = isTopNavVisible ? SafeAreaService.shared.topBarHeight : 0
        let bottomNavHeight = isBottomNavVisible ? SafeAreaService.shared.bottomBarHeight : 0

        let safeTop = statusBarHeight + topNavHeight
        let safeBottom = rootSafeArea.bottom + bottomNavHeight

        return [
            "statusBar": [
                "height": round2(statusBarHeight),
                "visible": !isStatusBarHidden
            ],
            "systemNavigation": [
                "height": round2(rootSafeArea.bottom),
                "visible": rootSafeArea.bottom > 0
            ],
            "keyboard": [
                "height": 0,
                "visible": false
            ],
            "safeArea": [
                "top": round2(safeTop),
                "right": round2(rootSafeArea.right),
                "bottom": round2(safeBottom),
                "left": round2(rootSafeArea.left)
            ]
        ]
    }

    private func round2(_ value: CGFloat) -> Double {
        (Double(value) * 100).rounded() / 100
    }
}
