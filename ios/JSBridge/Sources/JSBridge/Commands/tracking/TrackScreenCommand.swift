import Foundation
import Orchard

/// Command for tracking screen views (fire-and-forget)
///
/// **Why separate from trackEvent?**
/// - Screen views are a special type of analytics event
/// - Many analytics services treat page views differently than events
/// - Allows different handling/routing in analytics pipeline
/// - Follows standard analytics SDK patterns (screenView vs event)
///
/// **Design Decision:**
/// Fire-and-forget like trackEvent. Screen tracking is observational
/// and shouldn't impact user experience if it fails.
///
/// **Integration:**
/// Uses C24Tracker with Firebase's standard screen_view event name,
/// following Firebase Analytics conventions for screen tracking.
public class TrackScreenCommand: BridgeCommand {
    public let action = "trackScreen"

    public init() {}

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

/// Wrapper to make bridge screen events conform to TrackingEvent protocol
private struct BridgeScreenTrackingEvent: TrackingEvent {
    let name: String
    let parameters: [String: Any]
}

