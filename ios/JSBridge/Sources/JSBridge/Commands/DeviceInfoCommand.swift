import Foundation
import UIKit

/// Command for device information requests
public final class DeviceInfoCommand: BridgeCommand {
    public let action = "deviceInfo"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        return [
            "platform": "iOS",
            "osVersion": UIDevice.current.systemVersion,
            "sdkVersion": getSDKVersion(),
            "manufacturer": "Apple",
            "model": getDeviceIdentifier(),
        ]
    }
    
    private func getSDKVersion() -> String {
        if let deploymentTarget = Bundle.main.infoDictionary?["MinimumOSVersion"] as? String {
            return deploymentTarget
        }
        return UIDevice.current.systemVersion
    }
    
    nonisolated private func getDeviceIdentifier() -> String {
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
