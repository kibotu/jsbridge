import Foundation

/// Command for refresh (currently a no-op placeholder)
public final class RefreshCommand: BridgeCommand {
    public let action = "refresh"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        return nil
    }
}
