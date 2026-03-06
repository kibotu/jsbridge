import Foundation

/// Handles `themeChanged` commands from web content.
///
/// Updates `ThemeService.shared` so the native app can observe
/// and apply the theme change.
public class ThemeChangedCommand: BridgeCommand {
    public let action = "themeChanged"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        let theme = content?["theme"] as? String ?? "dark"
        ThemeService.shared.setTheme(theme)
        return nil
    }
}
