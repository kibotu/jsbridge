import Foundation
import AVFoundation
import CoreLocation
import Photos
import Orchard

/// Requests system permissions on behalf of web content.
///
/// Web code cannot directly request iOS permissions (camera, location, photos).
/// This bridges the permission model gap, allowing web apps to trigger native
/// permission dialogs for device capabilities that require explicit user consent.
///
/// Supported permission types: "camera", "microphone", "photoLibrary", "location".
public class RequestPermissionsCommand: BridgeCommand {
    public let action = "requestPermissions"

    public init() {}

    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let permissions = content?["permissions"] as? [String], !permissions.isEmpty else {
            throw BridgeError.invalidParameter("permissions")
        }

        Orchard.v("[Bridge] Requested permissions: \(permissions)")

        var results: [String: String] = [:]
        for permission in permissions {
            results[permission] = await requestPermissionAsync(permission)
        }

        let allGranted = results.values.allSatisfy { $0 == "granted" }
        return [
            "granted": allGranted,
            "permissions": results
        ]
    }

    private func requestPermissionAsync(_ permission: String) async -> String {
        await withCheckedContinuation { continuation in
            switch permission {
            case "camera":
                AVCaptureDevice.requestAccess(for: .video) { granted in
                    continuation.resume(returning: granted ? "granted" : "denied")
                }
            case "microphone":
                AVCaptureDevice.requestAccess(for: .audio) { granted in
                    continuation.resume(returning: granted ? "granted" : "denied")
                }
            case "photoLibrary":
                PHPhotoLibrary.requestAuthorization { status in
                    continuation.resume(returning: status == .authorized || status == .limited ? "granted" : "denied")
                }
            case "location":
                continuation.resume(returning: "unsupported")
            default:
                Orchard.w("[Bridge] Unknown permission: \(permission)")
                continuation.resume(returning: "unknown")
            }
        }
    }
}
