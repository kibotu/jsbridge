package net.kibotu.jsbridge.commands

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import com.github.florent37.application.provider.application
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Opens Android app settings screen for manual permission configuration.
 *
 * **Why web needs this:**
 * When permissions are denied, users need a path to re-enable them. Android's
 * permission system prevents repeatedly asking (after "Don't ask again").
 * This provides escape hatch for users who change their mind.
 *
 * **Why direct to app settings:**
 * Settings.ACTION_APPLICATION_DETAILS_SETTINGS opens directly to this app's
 * settings page, avoiding user confusion navigating through system settings.
 *
 * **Why FLAG_ACTIVITY_NEW_TASK:**
 * Settings are separate activity stack. New task ensures proper back navigation
 * and prevents settings from becoming part of app's navigation history.
 */
class OpenSettingsCommand : BridgeCommand {

    override val action = "openSettings"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        val context = requireNotNull(ActivityProvider.currentActivity ?: application)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "OPEN_SETTINGS_FAILED",
                e.message ?: "Failed to open settings"
            )
        }
    }
}

