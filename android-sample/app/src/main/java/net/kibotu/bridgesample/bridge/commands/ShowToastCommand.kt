package net.kibotu.bridgesample.bridge.commands

import android.widget.Toast
import net.kibotu.bridgesample.bridge.commands.utils.BridgeParsingUtils
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import com.github.florent37.application.provider.application
import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Displays Android Toast messages for lightweight, non-intrusive user feedback.
 *
 * **Why web needs this:**
 * Web has no equivalent to Android Toasts. Alternatives are:
 * - Alert dialogs: too intrusive, require dismissal
 * - Snackbars in HTML: look non-native, don't persist across pages
 * Native toasts provide familiar, platform-consistent feedback pattern.
 *
 * **Why Toast over Snackbar:**
 * Toast for simple notifications (saved, copied, sent). Toasts:
 * - Auto-dismiss without user action
 * - Appear over all UI (even during transitions)
 * - Familiar pattern Android users expect
 *
 * **Common use cases:**
 * "Copied to clipboard", "Settings saved", "Message sent" - quick confirmations
 * that don't require user acknowledgment.
 *
 * **Why app context fallback:**
 * If no Activity is available (rare edge case), app context works for Toasts.
 * Ensures toast can always be shown.
 */
class ShowToastCommand : BridgeCommand {

    override val action = "showToast"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        val message = BridgeParsingUtils.parseString(content, "message")
        if (message.isEmpty()) {
            return@withContext BridgeResponseUtils.createErrorResponse(
                "INVALID_PARAMETER",
                "Missing 'message' parameter"
            )
        }

        try {
            val context = ActivityProvider.currentActivity ?: application
            val duration = BridgeParsingUtils.parseDuration(content)
            Toast.makeText(context, message, duration).show()
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "TOAST_FAILED",
                e.message ?: "Failed to show toast"
            )
        }
    }
}

