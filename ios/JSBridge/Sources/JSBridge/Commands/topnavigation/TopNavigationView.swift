import SwiftUI

/// Configuration for the top navigation bar
public struct TopNavigationConfig: Sendable {
    public var isVisible: Bool = true
    public var title: String? = "Bridge Demo"
    public var showBackButton: Bool = false
    public var showDivider: Bool = true
    public var showLogo: Bool = false
    public var showProfileIconWidget: Bool = false

    public init(
        isVisible: Bool = true,
        title: String? = "Bridge Demo",
        showBackButton: Bool = false,
        showDivider: Bool = true,
        showLogo: Bool = false,
        showProfileIconWidget: Bool = false
    ) {
        self.isVisible = isVisible
        self.title = title
        self.showBackButton = showBackButton
        self.showDivider = showDivider
        self.showLogo = showLogo
        self.showProfileIconWidget = showProfileIconWidget
    }
}

/// Observable service for top navigation state
@MainActor
public final class TopNavigationService: ObservableObject {
    @Published public var config = TopNavigationConfig()

    public static let shared = TopNavigationService()

    private init() {}

    public func configure(with config: TopNavigationConfig) {
        self.config = config
    }

    public func update(isVisible: Bool? = nil,
                title: String? = nil,
                showBackButton: Bool? = nil,
                showDivider: Bool? = nil,
                showLogo: Bool? = nil,
                showProfileIconWidget: Bool? = nil) {
        withAnimation(.easeInOut(duration: 0.3)) {
            if let isVisible = isVisible {
                self.config.isVisible = isVisible
            }
            if let title = title {
                self.config.title = title
            }
            if let showBackButton = showBackButton {
                self.config.showBackButton = showBackButton
            }
            if let showDivider = showDivider {
                self.config.showDivider = showDivider
            }
            if let showLogo = showLogo {
                self.config.showLogo = showLogo
            }
            if let showProfileIconWidget = showProfileIconWidget {
                self.config.showProfileIconWidget = showProfileIconWidget
            }
        }
    }
}

extension Color {
    public init(light: Color, dark: Color) {
        self.init(UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
    }

    public static let accentBlue = Color(
        light: Color(red: 5/255, green: 99/255, blue: 193/255),
        dark: Color(red: 5/255, green: 99/255, blue: 193/255)
    )

    public static let surfaceColor = Color(
        light: .white,
        dark: Color(red: 30/255, green: 41/255, blue: 59/255)
    )

    public static let slateBackground = Color(
        light: Color(red: 248/255, green: 250/255, blue: 252/255),
        dark: Color(red: 15/255, green: 23/255, blue: 42/255)
    )

    public static let onSurfaceVariant = Color(
        light: Color(red: 71/255, green: 85/255, blue: 105/255),
        dark: Color(red: 203/255, green: 213/255, blue: 225/255)
    )
}
