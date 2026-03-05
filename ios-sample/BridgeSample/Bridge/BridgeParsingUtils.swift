import Foundation

/// Typed extraction helpers for bridge command content dictionaries.
/// Mirrors Android's BridgeParsingUtils for consistent parameter parsing across platforms.
extension Dictionary where Key == String, Value == Any {

    func string(_ key: String) -> String? {
        self[key] as? String
    }

    func int(_ key: String) -> Int? {
        self[key] as? Int
    }

    func double(_ key: String) -> Double? {
        self[key] as? Double
    }

    func bool(_ key: String) -> Bool? {
        self[key] as? Bool
    }

    func stringArray(_ key: String) -> [String]? {
        self[key] as? [String]
    }

    func dict(_ key: String) -> [String: Any]? {
        self[key] as? [String: Any]
    }

    func requireString(_ key: String) throws -> String {
        guard let value = self[key] as? String, !value.isEmpty else {
            throw BridgeError.invalidParameter(key)
        }
        return value
    }

    func requireBool(_ key: String) throws -> Bool {
        guard let value = self[key] as? Bool else {
            throw BridgeError.invalidParameter(key)
        }
        return value
    }

    func requireStringArray(_ key: String) throws -> [String] {
        guard let value = self[key] as? [String], !value.isEmpty else {
            throw BridgeError.invalidParameter(key)
        }
        return value
    }
}
