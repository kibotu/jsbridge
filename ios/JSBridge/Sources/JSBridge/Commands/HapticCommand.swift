import Foundation
import UIKit

/// Command for haptic feedback
public final class HapticCommand: BridgeCommand {
    public let action = "haptic"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let vibrate = content?["vibrate"] as? Bool, vibrate else {
            return nil
        }
        
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
        return nil
    }
}
