import SwiftUI
import Orchard

@MainActor
class ThemeManager: ObservableObject {
    static let shared = ThemeManager()
    @Published var isDarkMode: Bool

    private init() {
        isDarkMode = UITraitCollection.current.userInterfaceStyle == .dark
    }

    func toggle() {
        isDarkMode.toggle()
    }

    var colorScheme: ColorScheme {
        isDarkMode ? .dark : .light
    }
}

@main
struct BridgeSampleApp: App {
    @StateObject private var themeManager = ThemeManager.shared

    init() {
        let consoleLogger = ConsoleLogger()
        consoleLogger.showTimesStamp = false
        consoleLogger.showInvocation = true
        Orchard.loggers.append(consoleLogger)
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .environmentObject(themeManager)
                .preferredColorScheme(themeManager.colorScheme)
        }
    }
}
