import Foundation
import UIKit
import SwiftUI
import Orchard

/// Shared state manager for system UI visibility
@MainActor
public final class SystemUIState: ObservableObject {
    public static let shared = SystemUIState()
    
    @Published public var isStatusBarHidden: Bool = false
    
    private init() {}
}

/// Command for system bars (status bar and navigation bar)
public final class SystemBarsCommand: BridgeCommand {
    public let action = "systemBars"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let content = content, let showStatusBar = content["showStatusBar"] as? Bool else {
            throw BridgeError.invalidParameter("showStatusBar")
        }
        
        Orchard.v("[Bridge] System bars command: showStatusBar=\(showStatusBar)")
        
        withAnimation(.easeInOut(duration: 0.2)) {
            SystemUIState.shared.isStatusBarHidden = !showStatusBar
        }
        return nil
    }
}
