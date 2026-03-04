import SwiftUI
import Orchard

/// Main tab view with bottom navigation
struct MainTabView: View {
    @EnvironmentObject var themeManager: ThemeManager
    @State private var currentBridge: JavaScriptBridge?
    @ObservedObject private var bottomNavService = BottomNavigationService.shared
    @ObservedObject private var topNavService = TopNavigationService.shared
    @ObservedObject private var tabNavService = TabNavigationService.shared
    @ObservedObject private var systemUIState = SystemUIState.shared
    
    private var safeAreaEdgesToIgnore: Edge.Set {
        return .all
    }
    
    private var bottomSafeAreaInset: CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.bottom ?? 0
    }
    
    var body: some View {
        VStack(spacing: 0) {
            TopNavigationView(onBackPressed: {
                Orchard.v("[MainTabView] Back button pressed")
            })
            
            ZStack {
                if tabNavService.selectedTab == 0 {
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
                    .transition(.opacity)
                }
                
                if tabNavService.selectedTab == 1 {
                    WebViewScreen(
                        url: URL(string: "https://kibotu.net/check24/jenkins/safearea/")!,
                        onBridgeReady: { bridge in
                            currentBridge = bridge
                            Orchard.v("[MainTabView] Bridge ready for Tab 2")
                        },
                        shouldRespectTopSafeArea: !topNavService.config.isVisible,
                        shouldRespectBottomSafeArea: !bottomNavService.config.isVisible
                    )
                    .transition(.opacity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            
            if bottomNavService.config.isVisible {
                VStack(spacing: 0) {
                    Divider()
                    
                    HStack(spacing: 0) {
                        TabBarItem(
                            icon: "house.fill",
                            label: "Home",
                            isSelected: tabNavService.selectedTab == 0
                        ) {
                            tabNavService.switchToTab(0)
                        }
                        
                        TabBarItem(
                            icon: "globe",
                            label: "Web",
                            isSelected: tabNavService.selectedTab == 1
                        ) {
                            tabNavService.switchToTab(1)
                        }
                    }
                    .frame(height: 50)
                    .padding(.bottom, bottomSafeAreaInset)
                    .background(Color.surfaceColor)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .background(Color.slateBackground)
        .edgesIgnoringSafeArea(safeAreaEdgesToIgnore)
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
    }
    
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

/// Custom tab bar item
struct TabBarItem: View {
    let icon: String
    let label: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                    .foregroundColor(isSelected ? .accentBlue : .onSurfaceVariant)
                
                Text(label)
                    .font(.system(size: 10))
                    .foregroundColor(isSelected ? .accentBlue : .onSurfaceVariant)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
    }
}

