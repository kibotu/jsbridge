import Foundation
import Security

/// Handler for secure data storage (using Keychain)
///
/// **Why provide secure storage to web content?**
/// - Web's localStorage is not encrypted and can be inspected
/// - Web's sessionStorage disappears when the page unloads
/// - Keychain provides OS-level encryption and security
/// - Enables storing sensitive data (tokens, keys) safely
///
/// **Design Decision:**
/// Uses iOS Keychain rather than UserDefaults because:
/// - Keychain data is encrypted by the OS
/// - Keychain survives app reinstalls (if configured)
/// - Keychain is the recommended way to store sensitive data on iOS
///
/// **Security Note:**
/// The bridge exposes Keychain to web content. This is acceptable because:
/// - The web content is trusted (part of the same app/organization)
/// - Keys are namespaced to prevent collision with native storage
/// - This is required for hybrid app architecture where web needs persistent secure storage
class SaveSecureDataHandler: BridgeCommand {
    let actionName = "saveSecureData"
    
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let key = content?["key"] as? String,
              let value = content?["value"] as? String else {
            throw BridgeError.invalidParameter("key or value")
        }
        
        guard KeychainHelper.save(key: key, value: value) else {
            throw BridgeError.internalError("Failed to save to keychain")
        }
        return nil
    }
}

