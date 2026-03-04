import Foundation

/// Represents a message exchanged between JavaScript and native code
///
/// **Why this structure?**
/// - **Version Control**: The schema version enables forward/backward compatibility as the bridge evolves
/// - **Request-Response Pattern**: The ID field enables matching asynchronous responses to requests
/// - **Type Safety**: Codable ensures both sides agree on the message format
///
/// **Design Decision:**
/// Keeping the message structure flat and simple makes it easier to:
/// - Debug by inspecting JSON in browser dev tools
/// - Parse on both JavaScript and native sides
/// - Version incrementally without breaking existing clients
struct JavaScriptBridgeMessage: Codable {
    /// Schema version for compatibility checking
    ///
    /// **Why needed?** Allows graceful degradation when web and native code are out of sync.
    /// For example, when users have an old app version but the web content is updated.
    let version: Int
    
    /// Unique identifier for request-response correlation
    ///
    /// **Why needed?** Enables asynchronous request-response pattern. Multiple JavaScript calls
    /// can be in flight simultaneously, and responses must be matched to the correct promise.
    let id: String
    
    /// The message payload
    let data: MessageData
    
    struct MessageData: Codable {
        /// The action/command to execute
        let action: String
        
        /// Optional content/parameters for the action
        let content: [String: AnyCodable]?
        
        enum CodingKeys: String, CodingKey {
            case action
            case content
        }
        
        init(action: String, content: [String: AnyCodable]? = nil) {
            self.action = action
            self.content = content
        }
        
        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            action = try container.decode(String.self, forKey: .action)
            content = try container.decodeIfPresent([String: AnyCodable].self, forKey: .content)
        }
    }
}

/// Helper for encoding/decoding dynamic JSON values
///
/// **Why needed?**
/// Swift's Codable doesn't natively support `Any` types. This wrapper enables:
/// - Passing dynamic data structures between JavaScript and native
/// - Supporting various JSON types (string, number, boolean, array, object)
/// - Maintaining type information during encode/decode cycles
///
/// **Design Trade-off:**
/// We lose compile-time type safety but gain flexibility. This is acceptable because:
/// - JavaScript is dynamically typed anyway
/// - Bridge handlers validate their specific parameters
/// - The alternative (defining every possible parameter type) is impractical
struct AnyCodable: Codable {
    let value: Any
    
    init(_ value: Any) {
        self.value = value
    }
    
    init(from decoder: Decoder) throws {
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
    
    func encode(to encoder: Encoder) throws {
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

