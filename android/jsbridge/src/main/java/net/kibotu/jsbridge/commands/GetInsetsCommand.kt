package net.kibotu.jsbridge.commands

import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kibotu.jsbridge.BridgeContextProvider
import net.kibotu.jsbridge.JavaScriptBridge
import net.kibotu.jsbridge.SafeAreaService
import net.kibotu.jsbridge.commands.bottomnavigation.BottomNavigationService
import net.kibotu.jsbridge.commands.topnavigation.TopNavigationService
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import org.json.JSONObject
import timber.log.Timber
import java.math.RoundingMode

/**
 * Returns current system inset values so web content can adapt its layout.
 *
 * All heights are in density-independent pixels (dp) for cross-platform
 * parity with iOS points and consistency with [SafeAreaService] CSS vars.
 *
 * `safeArea` represents the total safe area insets that content must respect,
 * including both system bars and native app chrome (top/bottom navigation).
 *
 * Web usage:
 * ```javascript
 * const insets = await jsbridge.call('insets');
 * ```
 */
class GetInsetsCommand : BridgeCommand, BridgeAware {

    override var bridge: JavaScriptBridge? = null

    override val action = "insets"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val activity = BridgeContextProvider.findActivity(bridge?.context)
                ?: return@withContext BridgeResponseUtils.createErrorResponse(
                    "NO_ACTIVITY",
                    "No active activity"
                )

            val density = activity.resources.displayMetrics.density
            val decorView = activity.window.decorView
            val rootInsets = decorView.rootWindowInsets

            val result = JSONObject()

            if (rootInsets != null) {
                val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(rootInsets, decorView)
                val statusBars = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
                val navBars = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
                val ime = insetsCompat.getInsets(WindowInsetsCompat.Type.ime())

                val statusBarDp = pxToDp(statusBars.top, density)
                val systemNavDp = pxToDp(navBars.bottom, density)
                val imeDp = pxToDp(ime.bottom, density)

                val isTopNavVisible = TopNavigationService.currentConfig().isVisible
                val isBottomBarVisible = BottomNavigationService.currentVisibility()

                val topNavDp = if (isTopNavVisible) SafeAreaService.topBarHeightDp.toDouble() else 0.0
                val bottomNavDp = if (isBottomBarVisible) SafeAreaService.bottomBarHeightDp.toDouble() else 0.0

                result.put("statusBar", JSONObject().apply {
                    put("height", statusBarDp)
                    put("visible", insetsCompat.isVisible(WindowInsetsCompat.Type.statusBars()))
                })
                result.put("systemNavigation", JSONObject().apply {
                    put("height", systemNavDp)
                    put("visible", insetsCompat.isVisible(WindowInsetsCompat.Type.navigationBars()))
                })
                result.put("keyboard", JSONObject().apply {
                    put("height", imeDp)
                    put("visible", insetsCompat.isVisible(WindowInsetsCompat.Type.ime()))
                })
                result.put("safeArea", JSONObject().apply {
                    put("top", (statusBarDp + topNavDp).roundTo2())
                    put("right", pxToDp(navBars.right, density))
                    put("bottom", (systemNavDp + bottomNavDp).roundTo2())
                    put("left", pxToDp(navBars.left, density))
                })
            } else {
                result.put("statusBar", JSONObject().apply {
                    put("height", 0)
                    put("visible", true)
                })
                result.put("systemNavigation", JSONObject().apply {
                    put("height", 0)
                    put("visible", true)
                })
                result.put("keyboard", JSONObject().apply {
                    put("height", 0)
                    put("visible", false)
                })
                result.put("safeArea", JSONObject().apply {
                    put("top", 0)
                    put("right", 0)
                    put("bottom", 0)
                    put("left", 0)
                })
            }

            Timber.i("[handle] insets=$result")
            result
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "INSETS_FAILED",
                e.message ?: "Failed to get insets"
            )
        }
    }

    private fun pxToDp(px: Int, density: Float): Double =
        (px / density).toDouble().roundTo2()

    private fun Double.roundTo2(): Double =
        toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
}
