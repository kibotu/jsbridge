import Foundation
import UIKit

/// Read-only query for current system bar dimensions and visibility.
///
/// Web content cannot measure native system bars from within the WebView sandbox.
/// This enables web to:
/// - Calculate available viewport space for layout decisions
/// - Adjust content positioning relative to native chrome
/// - Make visibility-aware UI decisions without trial-and-error
///
/// All heights are in points (pt) which map 1:1 to dp on Android,
/// ensuring cross-platform parity.
///
/// Response shape (matches Android `SystemBarsInfoCommand`):
/// ```json
/// {
///   "statusBar":        { "height": 54.0, "isVisible": true },
///   "topNavigation":    { "height": 44.0, "isVisible": true },
///   "bottomNavigation": { "height": 50.0, "isVisible": false },
///   "systemNavigation": { "height": 34.0, "isVisible": true }
/// }
/// ```
///
/// `@unchecked Sendable` because the weak `bridge` ref is only accessed
/// on `@MainActor`.
public final class SystemBarsInfoCommand: BridgeCommand, BridgeAware, @unchecked Sendable {
    public let action = "systemBarsInfo"

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

        let topNavConfig = TopNavigationService.shared.config
        let bottomNavConfig = BottomNavigationService.shared.config

        let topNavHeight = topNavConfig.isVisible ? SafeAreaService.shared.topBarHeight : 0
        let bottomNavHeight = bottomNavConfig.isVisible ? SafeAreaService.shared.bottomBarHeight : 0

        return [
            "statusBar": [
                "height": round2(statusBarHeight),
                "isVisible": !isStatusBarHidden
            ],
            "topNavigation": [
                "height": round2(topNavHeight),
                "isVisible": topNavConfig.isVisible
            ],
            "bottomNavigation": [
                "height": round2(bottomNavHeight),
                "isVisible": bottomNavConfig.isVisible
            ],
            "systemNavigation": [
                "height": round2(rootSafeArea.bottom),
                "isVisible": rootSafeArea.bottom > 0
            ]
        ]
    }

    private func round2(_ value: CGFloat) -> Double {
        (Double(value) * 100).rounded() / 100
    }
}
