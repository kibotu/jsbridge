import SwiftUI
import Orchard
import JSBridge

/// Main tab view with bottom navigation
struct MainTabView: View {
    @EnvironmentObject var themeManager: ThemeManager
    @State private var selectedTab = 0
    @State private var currentBridge: JavaScriptBridge?
    @ObservedObject private var bottomNavService = BottomNavigationService.shared
    @ObservedObject private var topNavService = TopNavigationService.shared
    @ObservedObject private var systemUIState = SystemUIState.shared
    @ObservedObject private var themeService = ThemeService.shared

    var body: some View {
        TabView(selection: $selectedTab) {
            homeTab
                .tag(0)
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }

            webTab
                .tag(1)
                .tabItem {
                    Label("Web", systemImage: "globe")
                }
        }
        .accentColor(.accentBlue)
        .onReceive(bottomNavService.$config) { config in
            setTabBarHidden(!config.isVisible)
        }
        .statusBar(hidden: systemUIState.isStatusBarHidden)
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            currentBridge?.notifyLifecycleEvent("focused")
            SafeAreaService.shared.pushToBridge(currentBridge)
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)) { _ in
            currentBridge?.notifyLifecycleEvent("defocused")
        }
        .onReceive(themeManager.$isDarkMode.dropFirst()) { isDark in
            currentBridge?.sendToWeb(action: "themeChanged", content: [
                "theme": isDark ? "dark" : "light"
            ])
        }
        .onReceive(themeService.$requestedTheme.dropFirst()) { theme in
            themeManager.isDarkMode = (theme == "dark")
        }
    }

    // MARK: - Tabs

    private var homeTab: some View {
        VStack(spacing: 0) {
            TopNavigationView(onBackPressed: {
                Orchard.v("[MainTabView] Back button pressed")
            })

            WebViewScreen(
                url: getLocalFileURL(filename: "index.html"),
                onBridgeReady: { bridge in
                    currentBridge = bridge
                    Orchard.v("[MainTabView] Bridge ready for Tab 1")
                    startPushNotificationSimulation(bridge: bridge)
                },
                shouldRespectTopSafeArea: !topNavService.config.isVisible,
                shouldRespectBottomSafeArea: !bottomNavService.config.isVisible
            )
        }
        .background(Color.slateBackground)
        .ignoresSafeArea(.all, edges: .all)
    }

    private var webTab: some View {
        VStack(spacing: 0) {
            TopNavigationView(onBackPressed: {
                Orchard.v("[MainTabView] Back button pressed")
            })

            WebViewScreen(
                url: URL(string: "https://trail.services.kibotu.net/")!,
                onBridgeReady: { bridge in
                    currentBridge = bridge
                    Orchard.v("[MainTabView] Bridge ready for Tab 2")
                },
                shouldRespectTopSafeArea: !topNavService.config.isVisible,
                shouldRespectBottomSafeArea: !bottomNavService.config.isVisible
            )
        }
        .background(Color.slateBackground)
        .ignoresSafeArea(.all, edges: .all)
    }

    // MARK: - Tab Bar Visibility

    private func setTabBarHidden(_ hidden: Bool) {
        guard let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene }).first,
              let window = windowScene.windows.first,
              let tabBarController = findTabBarController(from: window.rootViewController)
        else { return }

        let tabBar = tabBarController.tabBar
        let screenHeight = window.frame.height
        let tabBarTop = tabBar.frame.origin.y
        let offset = screenHeight - tabBarTop

        UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut) {
            tabBar.transform = hidden ? CGAffineTransform(translationX: 0, y: offset) : .identity
        }
    }

    private func findTabBarController(from vc: UIViewController?) -> UITabBarController? {
        if let tbc = vc as? UITabBarController { return tbc }
        for child in vc?.children ?? [] {
            if let found = findTabBarController(from: child) { return found }
        }
        if let presented = vc?.presentedViewController {
            return findTabBarController(from: presented)
        }
        return nil
    }

    // MARK: - Helpers

    private func getLocalFileURL(filename: String) -> URL {
        if let url = Bundle.main.url(forResource: filename.replacingOccurrences(of: ".html", with: ""), withExtension: "html", subdirectory: "Resources") {
            return url
        }
        return Bundle.main.url(forResource: filename.replacingOccurrences(of: ".html", with: ""), withExtension: "html") ?? URL(string: "about:blank")!
    }

    private func startPushNotificationSimulation(bridge: JavaScriptBridge) {
        Timer.scheduledTimer(withTimeInterval: Double.random(in: 7...15), repeats: true) { _ in
            bridge.sendToWeb(action: "onPushNotification", content: [
                "url": "https://www.google.com",
                "message": "Lorem Ipsum"
            ])
        }
    }
}
