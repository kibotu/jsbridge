import Foundation
import Network

/// Command for network status checks
///
/// **Why expose network status to web?**
/// - Enables web content to adapt to offline/online state
/// - Allows preemptive error messages ("You appear to be offline")
/// - Supports offline-first strategies in web code
/// - Helps web decide whether to attempt network requests
/// - Enables web to optimize content/quality based on connection type
///
/// **Design Decision:**
/// Uses Apple's Network framework (iOS 12+) to monitor network connectivity.
/// This provides accurate, real-time network status information including
/// connection type detection (wifi, cellular, wired).
///
/// **Connection Type Detection:**
/// Returns the actual connection type (wifi, cellular, wired, or none)
/// by checking the NWPath's available interfaces. This is more reliable
/// than older Reachability APIs and is Apple's recommended approach.
///
/// **Implementation Note:**
/// Uses NWPathMonitor to get the current network state. The monitor provides
/// a snapshot of the current path which includes both reachability status
/// and the connection type information.
public class NetworkStatusCommand: BridgeCommand {
    public let action = "networkState"

    public init() {}

    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        return await withCheckedContinuation { continuation in
            let monitor = NWPathMonitor()
            let queue = DispatchQueue(label: "net.kibotu.networkstatus.monitor")
            
            monitor.pathUpdateHandler = { [weak self] path in
                let isConnected = path.status == .satisfied
                let connectionType = self?.determineConnectionType(from: path) ?? "unknown"
                
                monitor.cancel()
                
                continuation.resume(returning: [
                    "connected": isConnected,
                    "type": connectionType
                ])
            }
            
            monitor.start(queue: queue)
        }
    }
    
    /// Determines the connection type from the network path
    ///
    /// **Priority order:**
    /// 1. Cellular - if cellular interface is available
    /// 2. WiFi - if wifi interface is available
    /// 3. Wired - if wired ethernet is available
    /// 4. None - if no connection is available
    ///
    /// **Why this order?**
    /// Cellular is checked first because in multi-interface scenarios
    /// (like Personal Hotspot), cellular is typically the actual connection
    /// method even if wifi is technically enabled.
    private func determineConnectionType(from path: NWPath) -> String {
        if path.status != .satisfied {
            return "none"
        }
        
        // Check interfaces in priority order
        if path.usesInterfaceType(.cellular) {
            return "cellular"
        } else if path.usesInterfaceType(.wifi) {
            return "wifi"
        } else if path.usesInterfaceType(.wiredEthernet) {
            return "wired"
        } else if path.usesInterfaceType(.loopback) {
            // Loopback means local-only connectivity (no internet)
            return "none"
        } else {
            // Other interface types (like .other) exist but are uncommon
            return "unknown"
        }
    }
}

