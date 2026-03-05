import Foundation
import UIKit
import SwiftUI
import Orchard

/// Shared state manager for system UI visibility
///
/// **Why use ObservableObject?**
/// - SwiftUI views can observe changes and update automatically
/// - Thread-safe updates through @Published property wrapper
/// - Allows bridge handlers to trigger UI updates across the app
///
/// **Why a singleton?**
/// - Status bar state is global to the app
/// - Needs to be accessible from both bridge handlers and SwiftUI views
/// - Simpler than passing environment objects through the view hierarchy
class SystemUIState: ObservableObject {
    static let shared = SystemUIState()
    
    @Published var isStatusBarHidden: Bool = false
    
    private init() {}
}

/// Handler for system bars (status bar and navigation bar)
///
/// **iOS Implementation:**
/// Unlike Android, iOS status bar control in SwiftUI requires a reactive approach:
/// - Uses @Published state to trigger view updates
/// - Views observe SystemUIState and apply .statusBarHidden() modifier
/// - Changes are applied immediately when the published value updates
///
/// **Design Decision:**
/// We invert the showStatusBar parameter (Android shows, iOS hides) to match
/// the iOS API's .statusBarHidden() modifier naming convention.
class SystemBarsHandler: BridgeCommand {
    let actionName = "systemBars"
    
    @MainActor
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
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
