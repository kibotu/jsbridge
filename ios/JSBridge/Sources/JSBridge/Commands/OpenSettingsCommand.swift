import Foundation
import UIKit
import Orchard

/// Command for opening app settings
///
/// **Why allow web to open settings?**
/// - Enables permission prompts ("Grant camera access in Settings")
/// - Improves UX by directly linking to settings when permissions are denied
/// - Reduces user frustration (no need to manually find app in Settings)
///
/// **Design Decision:**
/// Opens the app's settings page in the Settings app, not system-wide settings.
/// This is the most common use case and prevents security concerns about
/// accessing arbitrary system settings.
///
/// **iOS Behavior:**
/// Takes user to the app's specific settings page where they can manage:
/// - Permissions (camera, location, notifications, etc.)
/// - App-specific preferences
/// - Storage management
public class OpenSettingsCommand: BridgeCommand {
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

