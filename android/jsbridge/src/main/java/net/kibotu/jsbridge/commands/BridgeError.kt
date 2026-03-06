package net.kibotu.jsbridge.commands

/**
 * Structured error type for the bridge layer.
 *
 * Mirrors the iOS `BridgeError` enum so both platforms produce identical
 * error codes over the wire. Prefer these over ad-hoc string pairs --
 * a sealed hierarchy gives exhaustive `when` branches and a single
 * place to grep for every error the bridge can produce.
 */
sealed class BridgeError(val code: String, val message: String) {
    object InvalidMessage : BridgeError("INVALID_MESSAGE", "Invalid message format")
    data class UnknownAction(val action: String) : BridgeError("UNKNOWN_ACTION", "Unknown action: $action")
    data class InvalidParameter(val param: String) : BridgeError("INVALID_PARAMETER", "Invalid or missing parameter: $param")
    data class InternalError(val msg: String) : BridgeError("INTERNAL_ERROR", "Internal error: $msg")
    data class UnsupportedVersion(val version: Int) : BridgeError("UNSUPPORTED_VERSION", "Unsupported schema version: $version")
}
