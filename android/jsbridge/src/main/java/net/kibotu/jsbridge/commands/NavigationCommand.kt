package net.kibotu.jsbridge.commands

import androidx.appcompat.app.AppCompatActivity
import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Handles multiple navigation patterns: back navigation, close, internal/external URLs.
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
                    val activity = ActivityProvider.currentActivity as? AppCompatActivity
                    if (activity == null) {
                        return@withContext BridgeResponseUtils.createErrorResponse(
                            "NO_ACTIVITY",
                            "No active activity"
                        )
                    }
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                close == true -> {
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
                    // URL navigation -- implement via your app's navigation service
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
