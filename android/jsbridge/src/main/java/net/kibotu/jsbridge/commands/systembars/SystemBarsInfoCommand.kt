package net.kibotu.jsbridge.commands.systembars

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kibotu.jsbridge.BridgeContextProvider
import net.kibotu.jsbridge.JavaScriptBridge
import net.kibotu.jsbridge.SafeAreaService
import net.kibotu.jsbridge.commands.BridgeAware
import net.kibotu.jsbridge.commands.BridgeCommand
import net.kibotu.jsbridge.commands.bottomnavigation.BottomNavigationService
import net.kibotu.jsbridge.commands.topnavigation.TopNavigationService
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import org.json.JSONObject
import timber.log.Timber
import java.math.RoundingMode

/**
 * Read-only query for current system bar dimensions and visibility.
 *
 * Web content cannot measure native system bars from within the WebView sandbox.
 * This enables web to:
 * - Calculate available viewport space for layout decisions
 * - Adjust content positioning relative to native chrome
 * - Make visibility-aware UI decisions without trial-and-error
 *
 * All heights are in density-independent pixels (dp) for cross-platform
 * parity with iOS points.
 *
 * Response shape (matches iOS `SystemBarsInfoCommand`):
 * ```json
 * {
 *   "statusBar":        { "height": 24.0, "isVisible": true },
 *   "topNavigation":    { "height": 56.0, "isVisible": true },
 *   "bottomNavigation": { "height": 48.0, "isVisible": false },
 *   "systemNavigation": { "height": 0.0,  "isVisible": false }
 * }
 * ```
 *
 * Web usage:
 * ```javascript
 * const info = await jsbridge.call('systemBarsInfo');
 * ```
 */
class SystemBarsInfoCommand : BridgeCommand, BridgeAware {

    override val action = "systemBarsInfo"

    override var bridge: JavaScriptBridge? = null

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val activity = BridgeContextProvider.findActivity(bridge?.context)
                ?: return@withContext BridgeResponseUtils.createErrorResponse(
                    "NO_ACTIVITY",
                    "No active activity"
                )

            val density = activity.resources.displayMetrics.density
            val rootView: View = activity.window.decorView

            val insets = ViewCompat.getRootWindowInsets(rootView)
            val systemBarInsets = insets?.getInsets(WindowInsetsCompat.Type.systemBars())

            val statusBarHeightDp = ((systemBarInsets?.top ?: 0) / density).roundTo2()
            val systemNavHeightDp = ((systemBarInsets?.bottom ?: 0) / density).roundTo2()

            val topNavConfig = TopNavigationService.currentConfig()
            val topNavHeightDp = if (topNavConfig.isVisible) SafeAreaService.topBarHeightDp.toDouble() else 0.0
            val isBottomNavVisible = BottomNavigationService.currentVisibility()

            val result = JSONObject().apply {
                put("statusBar", JSONObject().apply {
                    put("height", statusBarHeightDp)
                    put("isVisible", insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true)
                })
                put("topNavigation", JSONObject().apply {
                    put("height", topNavHeightDp)
                    put("isVisible", topNavConfig.isVisible)
                })
                put("bottomNavigation", JSONObject().apply {
                    put("height", if (isBottomNavVisible) SafeAreaService.bottomBarHeightDp.toDouble() else 0.0)
                    put("isVisible", isBottomNavVisible)
                })
                put("systemNavigation", JSONObject().apply {
                    put("height", systemNavHeightDp)
                    put("isVisible", systemNavHeightDp > 0)
                })
            }

            Timber.i("[SystemBarsInfoCommand] statusBar=${statusBarHeightDp}dp, topNav=${topNavHeightDp}dp, systemNav=${systemNavHeightDp}dp")
            result
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "SYSTEM_BARS_INFO_FAILED",
                e.message ?: "Failed to query system bars info"
            )
        }
    }

    private fun Float.roundTo2(): Double =
        toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
}
