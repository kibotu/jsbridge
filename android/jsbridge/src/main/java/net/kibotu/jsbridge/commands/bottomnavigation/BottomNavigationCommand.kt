package net.kibotu.jsbridge.commands.bottomnavigation

import net.kibotu.jsbridge.SafeAreaService
import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import net.kibotu.jsbridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

class BottomNavigationCommand(
    private val getBridge: () -> net.kibotu.jsbridge.JavaScriptBridge?
) : BridgeCommand {

    override val action = "bottomNavigation"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val isVisible = BridgeParsingUtils.parseBoolean(content, "isVisible")

            Timber.i("[handle] isVisible=$isVisible")

            BottomNavigationService.setVisible(isVisible == true)

            SafeAreaService.pushTobridge(getBridge())

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