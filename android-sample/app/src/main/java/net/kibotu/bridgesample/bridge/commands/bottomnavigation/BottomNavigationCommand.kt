package net.kibotu.bridgesample.bridge.commands.bottomnavigation

import net.kibotu.bridgesample.bridge.commands.utils.BridgeParsingUtils
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Controls visibility of app's bottom navigation bar from web content.
 *
 * **Why web needs this:**
 * Bottom navigation is native UI outside WebView. Web needs to control it for:
 * - Full-screen experiences (hide for immersive content)
 * - Flow isolation (hide during checkout/forms to reduce distractions)
 * - Context-specific UI (some web pages are leaf nodes, don't need tab navigation)
 *
 * **Why simple show/hide:**
 * Unlike top navigation, bottom nav rarely needs partial customization.
 * It's either part of the UI (shown) or shouldn't be (hidden). Simple boolean
 * covers all realistic use cases.
 *
 * **Why reactive (StateFlow):**
 * Service uses StateFlow allowing multiple observers to react to visibility
 * changes, enabling coordinated UI updates across native components.
 */
class BottomNavigationCommand : BridgeCommand {

    override val action = "bottomNavigation"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val isVisible = BridgeParsingUtils.parseBoolean(content, "isVisible")

            Timber.i("[handle] isVisible=$isVisible")

            BottomNavigationService.setVisible(isVisible == true)
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "BOTTOM_NAVIGATION_FAILED",
                e.message ?: "Failed to configure bottom navigation"
            )
        }
    }
}