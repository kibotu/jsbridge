import SwiftUI

/// Configuration for bottom navigation
public struct BottomNavigationConfig: Sendable {
    public var isVisible: Bool = true

    public init(isVisible: Bool = true) {
        self.isVisible = isVisible
    }
}

/// Observable service for bottom navigation state
@MainActor
public final class BottomNavigationService: ObservableObject {
    @Published public var config = BottomNavigationConfig()

    public static let shared = BottomNavigationService()

    private init() {}

    public func configure(with config: BottomNavigationConfig) {
        self.config = config
    }

    public func setVisible(_ isVisible: Bool) {
        withAnimation(.easeInOut(duration: 0.3)) {
            self.config.isVisible = isVisible
        }
    }
}
