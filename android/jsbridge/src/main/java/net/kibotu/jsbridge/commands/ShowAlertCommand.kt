package net.kibotu.jsbridge.commands

import androidx.appcompat.app.AlertDialog
import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Displays native Android AlertDialog for important user communications.
 *
 * **Why web needs this:**
 * Web's `alert()` and `confirm()` are:
 * - Blocked by most WebView implementations (security)
 * - Non-native appearance (breaks platform consistency)
 * - Limited customization
 * Native dialogs match Material Design and user expectations.
 *
 * **Why native alerts over web modals:**
 * - Native theming (follows system dark mode, colors)
 * - Accessibility built-in (screen readers, focus management)
 * - Proper lifecycle handling (survive orientation changes)
 * - Block interaction with underlying content (true modal)
 *
 * **Why customizable buttons:**
 * Different contexts need different actions: "OK/Cancel", "Yes/No", "Delete/Keep".
 * Flexible button text allows appropriate language for context.
 *
 * **Typical use cases:**
 * Confirmations before destructive actions, important notices, error messages
 * requiring acknowledgment.
 */
class ShowAlertCommand : BridgeCommand {

    override val action = "showAlert"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        val title = BridgeParsingUtils.parseString(content, "title")
        val message = BridgeParsingUtils.parseString(content, "message")
        val buttons = BridgeParsingUtils.parseStringArray(content, "buttons")

        if (message.isEmpty()) {
            return@withContext BridgeResponseUtils.createErrorResponse(
                "INVALID_PARAMETER",
                "Missing 'message' parameter"
            )
        }

        try {
            val activity = ActivityProvider.currentActivity
            if (activity == null) {
                return@withContext BridgeResponseUtils.createErrorResponse(
                    "NO_ACTIVITY",
                    "No active activity"
                )
            }

            AlertDialog.Builder(activity)
                .setTitle(title.ifEmpty { "Alert" })
                .setMessage(message)
                .apply {
                    if (buttons.isNotEmpty()) {
                        setPositiveButton(buttons.getOrNull(0) ?: "OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        if (buttons.size > 1) {
                            setNegativeButton(buttons.getOrNull(1)) { dialog, _ ->
                                dialog.dismiss()
                            }
                        }
                    } else {
                        setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                    }
                }
                .show()

            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "ALERT_FAILED",
                e.message ?: "Failed to show alert"
            )
        }
    }
}