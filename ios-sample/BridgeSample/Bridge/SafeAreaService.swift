import UIKit

/// Computes and pushes safe area CSS custom properties to the WebView.
///
/// Called from multiple sites matching the Android provider pattern:
/// - didFinish navigation (initial load + navigations)
/// - TopNavigationHandler (after toggling top bar)
/// - BottomNavigationHandler (after toggling bottom bar)
/// - Window focus gain (returning from another screen/app)
class SafeAreaService {
    static let shared = SafeAreaService()

    var topBarHeight: CGFloat = 44
    var bottomBarHeight: CGFloat = 50

    private init() {}

    func pushToBridge(_ bridge: JavaScriptBridge?) {
        guard let bridge = bridge else { return }

        let topConfig = TopNavigationService.shared.config
        let bottomConfig = BottomNavigationService.shared.config

        let statusBarHeight: CGFloat
        if let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene }).first {
            statusBarHeight = windowScene.statusBarManager?.statusBarFrame.height ?? 0
        } else {
            statusBarHeight = 0
        }

        // When top nav is visible, the VStack already places web content below it -- CSS inset = 0.
        // When hidden, edgesIgnoringSafeArea(.top) lets content extend under the status bar -- web needs the height.
        let effectiveTop = topConfig.isVisible ? 0 : statusBarHeight
        let effectiveBottom = bottomConfig.isVisible ? bottomBarHeight : 0

        bridge.updateSafeAreaCSS(
            insetTop: effectiveTop,
            insetBottom: effectiveBottom,
            statusBarHeight: statusBarHeight,
            topNavHeight: topConfig.isVisible ? topBarHeight : 0,
            bottomNavHeight: effectiveBottom,
            systemNavHeight: 0
        )
    }
}
