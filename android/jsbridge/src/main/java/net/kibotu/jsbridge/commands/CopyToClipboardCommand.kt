package net.kibotu.jsbridge.commands

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import com.github.florent37.application.provider.application
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Copies text to system clipboard for cross-app sharing.
 *
 * **Why web needs this:**
 * Web's clipboard API (`navigator.clipboard`) requires HTTPS and specific permissions.
 * In WebView context, it's unreliable. Native clipboard access ensures:
 * - Copy works in all contexts (HTTP, HTTPS, localhost)
 * - Consistent behavior across Android versions
 * - Integration with system clipboard for paste in other apps
 *
 * **Why "bridge_clipboard" label:**
 * ClipData requires a label for clipboard history UI. Generic label identifies
 * source as bridge without exposing sensitive content in label.
 *
 * **Common use cases:**
 * Sharing codes, URLs, account numbers, addresses - anything user needs to
 * paste elsewhere.
 */
class CopyToClipboardCommand : BridgeCommand {

    override val action = "copyToClipboard"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        val text = BridgeParsingUtils.parseString(content, "text")
        if (text.isEmpty()) {
            return@withContext BridgeResponseUtils.createErrorResponse(
                "INVALID_PARAMETER",
                "Missing 'text' parameter"
            )
        }

        val context = requireNotNull(ActivityProvider.currentActivity ?: application)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

        try {
            val clip = ClipData.newPlainText("bridge_clipboard", text)
            clipboard?.setPrimaryClip(clip)
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "CLIPBOARD_FAILED",
                e.message ?: "Failed to copy to clipboard"
            )
        }
    }
}

