import Foundation
import Combine

/// Observable service that publishes theme changes requested by web content.
///
/// The app observes `requestedTheme` and applies it to its own theme state.
/// This keeps JSBridge decoupled from any app-level ThemeManager.
@MainActor
public final class ThemeService: ObservableObject {
    public static let shared = ThemeService()

    @Published public var requestedTheme: String = "dark"

    private init() {}

    public func setTheme(_ theme: String) {
        self.requestedTheme = theme
    }
}
