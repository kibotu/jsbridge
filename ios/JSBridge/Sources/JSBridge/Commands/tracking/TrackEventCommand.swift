import Foundation
import Orchard

/// Command for tracking analytics events (fire-and-forget)
public final class TrackEventCommand: BridgeCommand {
    public let action = "trackEvent"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let event = content?["event"] as? String else {
            throw BridgeError.invalidParameter("event")
        }
        
        let params = content?["params"] as? [String: Any] ?? [:]
        
        let trackingEvent = BridgeTrackingEvent(name: event, parameters: params)
        Orchard.v("\(trackingEvent)")
        Orchard.v("[Bridge] Track event: \(event) with params: \(params)")
        
        return nil
    }
}

private struct BridgeTrackingEvent: TrackingEvent {
    let name: String
    let parameters: [String: Any]
}

public protocol TrackingEvent {
    var name: String { get }
    var parameters: [String: Any] { get }
}
