package net.kibotu.jsbridge.commands.refresh

import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Triggers app-wide refresh/sync operations from web content.
 *
 * **Why web needs this:**
 * Web can refresh its own data, but sometimes needs to trigger native refresh:
 * - User updates profile in web, native UI needs latest data
 * - Pull-to-refresh gesture on web should refresh native components too
 * - Data sync after web completes transaction
 * - Invalidate caches across native and web layers
 *
 * **Why optional command parameter:**
 * Allows targeted refresh instead of refreshing everything. Examples:
 * - "profile" - refresh only profile-related data
 * - "jobs" - refresh job listings
 * - null/empty - refresh everything
 * Enables performance optimization by avoiding unnecessary work.
 *
 * **Why through RefreshService:**
 * Centralized refresh coordination. Service notifies all interested observers
 * (fragments, view models, etc.) to refresh their data. Ensures consistent
 * refresh behavior across app.
 *
 * **Why Dispatchers.Main:**
 * Service interactions and StateFlow updates must happen on main thread.
 */
class RefreshCommand : BridgeCommand {

    override val action = "refresh"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val command = BridgeParsingUtils.parseString(content, "command")

            Timber.i("[RefreshCommand] Triggering refresh with command=$command")

            RefreshService.refresh(command)

            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "REFRESH_FAILED",
                e.message ?: "Failed to trigger refresh"
            )
        }
    }
}