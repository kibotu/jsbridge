import Foundation
import UIKit

/// Command for copying text to the system clipboard
public final class CopyToClipboardCommand: BridgeCommand {
    public let action = "copyToClipboard"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let text = content?["text"] as? String else {
            throw BridgeError.invalidParameter("text")
        }
        
        UIPasteboard.general.string = text
        return nil
    }
}
