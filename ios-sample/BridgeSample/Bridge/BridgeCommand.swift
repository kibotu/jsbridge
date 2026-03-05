import Foundation

/// Protocol defining a command handler for the JavaScript bridge.
///
/// **Why this pattern?**
/// - **Extensibility**: New commands can be added without modifying the core bridge logic
/// - **Single Responsibility**: Each handler focuses on one specific action
/// - **Testability**: Handlers can be tested in isolation without requiring a full WebView setup
/// - **Type Safety**: Ensures all handlers conform to the same contract
///
/// **Design Decision:**
/// Uses the Strategy pattern to allow runtime registration of handlers. This enables:
/// - Feature teams to add new commands independently
/// - Conditional handler registration based on app configuration
/// - Easy mocking and testing of individual command handlers
protocol BridgeCommand {
    /// The action name this handler responds to
    ///
    /// **Why a string?** JavaScript sends action names as strings, and using strings
    /// here maintains the 1:1 mapping between JS and native code.
    var actionName: String { get }
    
    /// Handle the command with the given content.
    ///
    /// Throw `BridgeError` for expected failures (invalid parameters, missing data).
    /// Return nil for fire-and-forget commands that don't need a response.
    ///
    /// - Parameter content: Optional dictionary of parameters from JavaScript
    /// - Returns: Response dictionary for web, or nil for fire-and-forget
    func handle(content: [String: Any]?) async throws -> [String: Any]?
}

/// Errors that can occur during bridge command handling
///
/// **Why a custom error type?**
/// - Provides structured error codes that JavaScript can programmatically handle
/// - Ensures consistent error messaging across the bridge boundary
/// - Makes debugging easier by categorizing different failure modes
enum BridgeError: Error {
    case invalidMessage
    case unknownAction(String)
    case invalidParameter(String)
    case internalError(String)
    case unsupportedVersion(Int)
    
    var code: String {
        switch self {
        case .invalidMessage:
            return "INVALID_MESSAGE"
        case .unknownAction:
            return "UNKNOWN_ACTION"
        case .invalidParameter:
            return "INVALID_PARAMETER"
        case .internalError:
            return "INTERNAL_ERROR"
        case .unsupportedVersion:
            return "UNSUPPORTED_VERSION"
        }
    }
    
    var message: String {
        switch self {
        case .invalidMessage:
            return "Invalid message format"
        case .unknownAction(let action):
            return "Unknown action: \(action)"
        case .invalidParameter(let param):
            return "Invalid or missing parameter: \(param)"
        case .internalError(let msg):
            return "Internal error: \(msg)"
        case .unsupportedVersion(let version):
            return "Unsupported schema version: \(version)"
        }
    }
}

