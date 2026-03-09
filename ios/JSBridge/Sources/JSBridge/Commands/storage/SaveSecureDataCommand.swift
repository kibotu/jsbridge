import Foundation
import Security

/// Command for secure data storage (using Keychain)
public final class SaveSecureDataCommand: BridgeCommand {
    public let action = "saveSecureData"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
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
