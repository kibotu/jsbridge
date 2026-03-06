import UIKit

/// Computes and pushes safe area CSS custom properties to the WebView.
///
/// Called from multiple sites matching the Android provider pattern:
/// - didFinish navigation (initial load + navigations)
/// - TopNavigationCommand (after toggling top bar)
/// - BottomNavigationCommand (after toggling bottom bar)
/// - Window focus gain (returning from another screen/app)
public class SafeAreaService {
    public static let shared = SafeAreaService()

    public var topBarHeight: CGFloat = 44
    public var bottomBarHeight: CGFloat = 50

    private init() {}

    public func pushToBridge(_ bridge: JavaScriptBridge?) {
        guard let bridge = bridge else { return }

        let topConfig = TopNavigationService.shared.config
        let bottomConfig = BottomNavigationService.shared.config

        let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene }).first
        let statusBarHeight = windowScene?.statusBarManager?.statusBarFrame.height ?? 0
        let bottomSafeArea = windowScene?.windows.first?.safeAreaInsets.bottom ?? 0

        // When top nav is visible, the VStack already places web content below it — CSS inset = 0.
        // When hidden, the web content extends under the status bar — CSS needs the height.
        let effectiveTop = topConfig.isVisible ? 0 : statusBarHeight

        // When bottom nav is visible, the VStack places web content above it — CSS inset = 0.
        // When hidden, the web content extends to the screen edge — CSS needs the bottom safe area.
        let effectiveBottom = bottomConfig.isVisible ? 0 : bottomSafeArea

        bridge.updateSafeAreaCSS(
            insetTop: effectiveTop,
            insetBottom: effectiveBottom,
            statusBarHeight: statusBarHeight,
            topNavHeight: topConfig.isVisible ? topBarHeight : 0,
            bottomNavHeight: bottomConfig.isVisible ? bottomBarHeight : 0,
            systemNavHeight: 0
        )
    }
}
