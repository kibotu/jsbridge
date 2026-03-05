package net.kibotu.jsbridge.commands

import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Opens URLs through app's internal navigation system (deep links).
 *
 * **Why web needs this:**
 * Web can use `<a href>` for same-page navigation, but for app-wide navigation
 * (switching screens, opening native features), it needs native navigation system.
 * This enables web to trigger deep links that route through app's navigation graph.
 *
 * **Why internal navigation:**
 * Uses app's NavigationService which handles deep link routing, ensuring web
 * content integrates seamlessly with native navigation patterns (back stack,
 * transitions, etc.).
 *
 * **Common patterns:**
 * - Opening profile screen: profis://profile
 * - Opening specific content: profis://job/123
 * - Triggering workflows: profis://onboarding/start
 */
class OpenUrlCommand : BridgeCommand {

    override val action = "openUrl"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        val url = BridgeParsingUtils.parseString(content, "url")
        if (url.isEmpty()) {
            return@withContext BridgeResponseUtils.createErrorResponse(
                "INVALID_PARAMETER",
                "Missing 'url' parameter"
            )
        }

        try {
//            CorePluginServices.services.navigationService.dispatch(url)
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "OPEN_URL_FAILED",
                e.message ?: "Failed to open URL"
            )
        }
    }
}

