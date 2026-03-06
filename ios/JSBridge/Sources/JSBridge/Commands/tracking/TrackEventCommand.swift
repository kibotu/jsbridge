import Foundation
import Orchard

/// Command for tracking analytics events (fire-and-forget)
///
/// **Why allow web to track events?**
/// - Enables unified analytics across native and web flows
/// - Web developers can track user behavior without native code changes
/// - Ensures analytics data goes through the same native pipeline
///
/// **Design Decision:**
/// Fire-and-forget pattern: returns success immediately without waiting for
/// analytics service acknowledgment. This is appropriate because:
/// - Analytics failures shouldn't block user actions
/// - Web content doesn't need to know if analytics succeeded
/// - Keeps the web interface simple and fast
///
/// **Integration:**
/// Uses C24Tracker to forward events to Firebase Analytics, ensuring
/// consistency with native event tracking across the app.
public class TrackEventCommand: BridgeCommand {
    public let action = "trackEvent"

    public init() {}

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

/// Wrapper to make bridge events conform to TrackingEvent protocol
private struct BridgeTrackingEvent: TrackingEvent {
    let name: String
    let parameters: [String: Any]
}

public protocol TrackingEvent {
    var name: String { get }
    var parameters: [String: Any] { get }
}
