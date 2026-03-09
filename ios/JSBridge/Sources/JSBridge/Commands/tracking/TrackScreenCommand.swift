import Foundation
import Orchard

/// Command for tracking screen views (fire-and-forget)
public final class TrackScreenCommand: BridgeCommand {
    public let action = "trackScreen"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let screenName = content?["screenName"] as? String else {
            throw BridgeError.invalidParameter("screenName")
        }
        
        let screenClass = content?["screenClass"] as? String
        
        var parameters: [String: Any] = ["screen_name": screenName]
        if let screenClass = screenClass {
            parameters["screen_class"] = screenClass
        }
        
        let trackingEvent = BridgeScreenTrackingEvent(name: "screen_view", parameters: parameters)
        Orchard.v("\(trackingEvent)")
        Orchard.v("[Bridge] Track screen: \(screenName), class: \(String(describing: screenClass))")
        
        return nil
    }
}

private struct BridgeScreenTrackingEvent: TrackingEvent {
    let name: String
    let parameters: [String: Any]
}
