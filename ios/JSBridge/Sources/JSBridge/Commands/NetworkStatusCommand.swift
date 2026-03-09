import Foundation
import Network

/// Command for network status checks
public final class NetworkStatusCommand: BridgeCommand {
    public let action = "networkState"

    public init() {}

    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        return await withCheckedContinuation { continuation in
            let monitor = NWPathMonitor()
            let queue = DispatchQueue(label: "net.kibotu.networkstatus.monitor")
            
            monitor.pathUpdateHandler = { path in
                let isConnected = path.status == .satisfied
                let connectionType = Self.determineConnectionType(from: path)
                
                monitor.cancel()
                
                continuation.resume(returning: [
                    "connected": isConnected,
                    "type": connectionType
                ])
            }
            
            monitor.start(queue: queue)
        }
    }
    
    nonisolated private static func determineConnectionType(from path: NWPath) -> String {
        if path.status != .satisfied {
            return "none"
        }
        
        if path.usesInterfaceType(.cellular) {
            return "cellular"
        } else if path.usesInterfaceType(.wifi) {
            return "wifi"
        } else if path.usesInterfaceType(.wiredEthernet) {
            return "wired"
        } else if path.usesInterfaceType(.loopback) {
            return "none"
        } else {
            return "unknown"
        }
    }
}
