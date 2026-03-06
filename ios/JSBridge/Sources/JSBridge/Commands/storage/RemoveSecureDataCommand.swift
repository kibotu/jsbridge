import Foundation

/// Command for removing secure data from Keychain
///
/// **Why have a separate remove command?**
/// - Makes the API explicit and clear (better than saving empty string)
/// - Properly deletes the Keychain item (not just overwriting with empty value)
/// - Prevents accumulation of unused Keychain items
///
/// **Design Decision:**
/// KeychainHelper.delete returns true even if the key doesn't exist (errSecItemNotFound).
/// This makes the operation idempotent - calling remove multiple times is safe.
public class RemoveSecureDataCommand: BridgeCommand {
    public let action = "removeSecureData"

    public init() {}

    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let key = content?["key"] as? String else {
            throw BridgeError.invalidParameter("key")
        }
        
        guard KeychainHelper.delete(key: key) else {
            throw BridgeError.internalError("Failed to remove from keychain")
        }
        return nil
    }
}

