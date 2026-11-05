import Foundation

/// Fills the `bridge.js` template placeholders.
enum BridgeScript {

    /// Sorted, de-duplicated feature names the given commands expose to `jsbridge.supports()`.
    static func featureNames(_ commands: [any BridgeCommand]) -> [String] {
        Array(commands.reduce(into: Set<String>()) { $0.formUnion($1.features) }).sorted()
    }

    static func render(_ template: String, name: String, commands: [any BridgeCommand]) -> String {
        let features = (try? JSONSerialization.data(withJSONObject: featureNames(commands)))
            .flatMap { String(data: $0, encoding: .utf8) } ?? "[]"
        return template
            .replacingOccurrences(of: "__BRIDGE_NAME__", with: name)
            .replacingOccurrences(of: "__FEATURES__", with: features)
    }
}
