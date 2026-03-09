import Foundation
import UIKit
import Orchard

/// Command for opening app settings
public final class OpenSettingsCommand: BridgeCommand {
    public let action = "openSettings"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
            throw BridgeError.internalError("Could not create settings URL")
        }
        
        guard UIApplication.shared.canOpenURL(settingsUrl) else {
            throw BridgeError.internalError("Cannot open settings URL")
        }
        
        let success = await UIApplication.shared.open(settingsUrl)
        guard success else {
            throw BridgeError.internalError("Failed to open settings")
        }
        return nil
    }
}
