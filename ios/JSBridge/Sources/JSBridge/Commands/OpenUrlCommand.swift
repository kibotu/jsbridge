import Foundation
import UIKit

/// Command for simple URL opening (always external / system browser)
public final class OpenUrlCommand: BridgeCommand {
    public let action = "openUrl"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let urlString = content?["url"] as? String,
              let url = URL(string: urlString) else {
            throw BridgeError.invalidParameter("url")
        }
        
        await UIApplication.shared.open(url)
        return nil
    }
}
