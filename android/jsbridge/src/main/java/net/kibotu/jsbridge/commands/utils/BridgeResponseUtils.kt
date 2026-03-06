package net.kibotu.jsbridge.commands.utils

import net.kibotu.jsbridge.commands.BridgeError
import org.json.JSONObject

/**
 * Utility class for creating standardized bridge command responses.
 */
object BridgeResponseUtils {

    /**
     * Creates a standardized error response from a [BridgeError].
     */
    fun createErrorResponse(error: BridgeError): JSONObject = createErrorResponse(error.code, error.message)

    /**
     * Creates a standardized error response from raw code/message strings.
     */
    fun createErrorResponse(code: String, message: String): JSONObject = JSONObject().apply {
        put("error", JSONObject().apply {
            put("code", code)
            put("message", message)
        })
    }

    /**
     * Creates a simple success response.
     */
    fun createSuccessResponse(): JSONObject = JSONObject()

}

