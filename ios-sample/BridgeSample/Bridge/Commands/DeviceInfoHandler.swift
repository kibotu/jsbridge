import Foundation
import UIKit

/// Handler for device information requests
///
/// **Why provide device info?**
/// - Enables web content to adapt based on platform capabilities
/// - Helps with debugging (knowing the exact device/OS version)
/// - Allows feature detection (iOS vs Android, version gating)
/// - Supports analytics correlation between native and web events
///
/// **Design Decision:**
/// Returns static device information synchronously. This avoids
/// async complexity since all this data is readily available.
class DeviceInfoHandler: BridgeCommand {
    let actionName = "deviceInfo"
    
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
        return [
            "platform": "iOS",
            "osVersion": UIDevice.current.systemVersion,
            "sdkVersion": getSDKVersion(),
            "manufacturer": "Apple",
            "model": getDeviceIdentifier(),
        ]
    }
    
    /// Returns the SDK version (deployment target) from the app bundle
    /// This is the iOS equivalent of Android's Build.VERSION.SDK_INT
    /// Note: iOS uses semantic versioning (e.g., "15.0") rather than integer API levels
    /// Returns the minimum iOS version the app supports (deployment target)
    private func getSDKVersion() -> String {
        // In iOS, the closest equivalent to Android's SDK_INT is the deployment target
        // This is the minimum iOS version the app was compiled to support
        if let deploymentTarget = Bundle.main.infoDictionary?["MinimumOSVersion"] as? String {
            return deploymentTarget
        }
        
        // Fallback: return the current OS version if deployment target not available
        return UIDevice.current.systemVersion
    }
    
    /// Returns the specific device identifier (e.g., "iPhone17,1" for iPhone 16 Pro)
    private func getDeviceIdentifier() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        return identifier
    }
}

