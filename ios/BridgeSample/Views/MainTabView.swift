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
        .statusBar(hidden: systemUIState.isStatusBarHidden)
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
        .tabBarHidden(!bottomNavService.config.isVisible, animated: true)
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
        .tabBarHidden(!bottomNavService.config.isVisible, animated: true)
    }

    // MARK: - Helpers

    private func getLocalFileURL(filename: String) -> URL {
        if let url = Bundle.main.url(forResource: filename.replacingOccurrences(of: ".html", with: ""), withExtension: "html", subdirectory: "Resources") {
            return url
        }
        return Bundle.main.url(forResource: filename.replacingOccurrences(of: ".html", with: ""), withExtension: "html") ?? URL(string: "about:blank")!
    }

    @MainActor
    private func startPushNotificationSimulation(bridge: JavaScriptBridge) {
        Timer.scheduledTimer(withTimeInterval: Double.random(in: 7...15), repeats: true) { _ in
            MainActor.assumeIsolated {
                bridge.sendToWeb(action: "onPushNotification", content: [
                    "url": "https://www.google.com",
                    "message": "Lorem Ipsum"
                ])
            }
        }
    }
}

// MARK: - Tab Bar Visibility Modifier

private struct TabBarHiddenModifier: ViewModifier {
    let isHidden: Bool
    let animated: Bool

    func body(content: Content) -> some View {
        content
            .toolbar(isHidden ? .hidden : .visible, for: .tabBar)
            .animation(animated ? .easeInOut(duration: 0.3) : nil, value: isHidden)
    }
}

extension View {
    func tabBarHidden(_ hidden: Bool, animated: Bool = true) -> some View {
        modifier(TabBarHiddenModifier(isHidden: hidden, animated: animated))
    }
}
