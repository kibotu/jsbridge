package net.kibotu.bridgesample.bridge.commands

import net.kibotu.bridgesample.bridge.commands.utils.BridgeParsingUtils
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import net.kibotu.bridgesample.misc.currentAppCompatActivity
import com.github.florent37.application.provider.ActivityProvider
import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Handles multiple navigation patterns: back navigation, close, internal/external URLs.
 *
 * **Why combined command:**
 * Navigation actions are mutually exclusive - user does one at a time. Single command
 * simplifies web API (one call instead of multiple) for common navigation patterns.
 *
 * **Why goBack:**
 * Allows web to trigger native back navigation (back stack), enabling web to
 * participate in Android's navigation system instead of fighting it.
 *
 * **Why close:**
 * Enables web to dismiss its own screen (finish activity), useful for modal
 * flows or when web determines its task is complete.
 *
 * **Why external option:**
 * Some URLs should open in browser (privacy policies, external sites) to make
 * clear they're leaving the app. External prevents deep link interception.
 *
 * **Why onBackPressedDispatcher:**
 * Uses modern Android back handling that respects OnBackPressedCallbacks,
 * allowing proper coordination with fragments, dialogs, etc.
 */
class NavigationCommand : BridgeCommand {

    override val action = "navigation"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val url = BridgeParsingUtils.parseString(content, "url")
            val external = BridgeParsingUtils.parseBoolean(content, "external")
            val goBack = BridgeParsingUtils.parseBoolean(content, "goBack")
            val close = BridgeParsingUtils.parseBoolean(content, "close")

            Timber.i("[handle] url=$url external=$external goBack=$goBack close=$close")

            when {
                goBack == true -> {
                    val activity = currentAppCompatActivity
                    if (activity == null) {
                        return@withContext BridgeResponseUtils.createErrorResponse(
                            "NO_ACTIVITY",
                            "No active activity"
                        )
                    }
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                close == true-> {
                    val activity = ActivityProvider.currentActivity
                    if (activity == null) {
                        return@withContext BridgeResponseUtils.createErrorResponse(
                            "NO_ACTIVITY",
                            "No active activity"
                        )
                    }
                    activity.finish()
                }

                url.isNotEmpty() -> {
                    if (external == true) {
                        // CorePluginServices.Companion.services.navigationService.dispatchInExternalBrowser(url)
                    } else {
                        // CorePluginServices.Companion.services.navigationService.dispatch(url, AppLinkSource.INTERNAL_PLUGIN)
                    }
                }

                else -> {
                    return@withContext BridgeResponseUtils.createErrorResponse(
                        "INVALID_PARAMETER",
                        "Missing navigation parameter"
                    )
                }
            }

            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "NAVIGATION_FAILED",
                e.message ?: "Failed to navigate"
            )
        }
    }
}