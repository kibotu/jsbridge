import Foundation
import UIKit

/// Handler for copying to clipboard
///
/// **Why provide clipboard access?**
/// - Enables "copy to clipboard" functionality in web content
/// - Better UX than web's Clipboard API (which requires user gestures)
/// - Allows copying of codes, links, addresses, etc. for user convenience
///
/// **Design Decision:**
/// Uses UIPasteboard.general for system-wide clipboard access.
/// This makes copied content available to all apps, which is the expected behavior.
///
/// **Security Note:**
/// Web content can copy anything to the clipboard. This is acceptable because:
/// - Web content is trusted (part of the app)
/// - User initiated the action (clicked a copy button)
/// - Alternative would be worse UX (forcing native implementation for every copy feature)
class CopyToClipboardHandler: BridgeCommand {
    let actionName = "copyToClipboard"
    
    @MainActor
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let text = content?["text"] as? String else {
            throw BridgeError.invalidParameter("text")
        }
        
        UIPasteboard.general.string = text
        return nil
    }
}

