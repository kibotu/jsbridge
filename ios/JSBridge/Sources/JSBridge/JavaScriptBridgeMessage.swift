import Foundation

/// Represents a message exchanged between JavaScript and native code
public struct JavaScriptBridgeMessage: Codable, Sendable {
    let version: Int
    let id: String
    let data: MessageData
    
    struct MessageData: Codable, Sendable {
        let action: String
        let content: [String: AnyCodable]?
        
        enum CodingKeys: String, CodingKey {
            case action
            case content
        }
        
        public init(action: String, content: [String: AnyCodable]? = nil) {
            self.action = action
            self.content = content
        }
        
        public init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            action = try container.decode(String.self, forKey: .action)
            content = try container.decodeIfPresent([String: AnyCodable].self, forKey: .content)
        }
    }
}

/// Helper for encoding/decoding dynamic JSON values.
///
/// Swift's Codable doesn't natively support `Any` types. This wrapper enables
/// passing dynamic data structures between JavaScript and native while
/// supporting various JSON types (string, number, boolean, array, object).
public struct AnyCodable: Codable, @unchecked Sendable {
    let value: Any
    
    public init(_ value: Any) {
        self.value = value
    }
    
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        
        if let bool = try? container.decode(Bool.self) {
            value = bool
        } else if let int = try? container.decode(Int.self) {
            value = int
        } else if let double = try? container.decode(Double.self) {
            value = double
        } else if let string = try? container.decode(String.self) {
            value = string
        } else if let array = try? container.decode([AnyCodable].self) {
            value = array.map { $0.value }
        } else if let dictionary = try? container.decode([String: AnyCodable].self) {
            value = dictionary.mapValues { $0.value }
        } else if container.decodeNil() {
            value = NSNull()
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Cannot decode AnyCodable"
            )
        }
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        
        switch value {
        case let bool as Bool:
            try container.encode(bool)
        case let int as Int:
            try container.encode(int)
        case let double as Double:
            try container.encode(double)
        case let string as String:
            try container.encode(string)
        case let array as [Any]:
            try container.encode(array.map { AnyCodable($0) })
        case let dictionary as [String: Any]:
            try container.encode(dictionary.mapValues { AnyCodable($0) })
        case is NSNull:
            try container.encodeNil()
        default:
            let context = EncodingError.Context(
                codingPath: container.codingPath,
                debugDescription: "Cannot encode value of type \(type(of: value))"
            )
            throw EncodingError.invalidValue(value, context)
        }
    }
}
