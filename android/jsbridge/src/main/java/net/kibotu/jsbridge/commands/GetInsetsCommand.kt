package net.kibotu.jsbridge.commands

import android.os.Build
import android.view.WindowInsets
import com.github.florent37.application.provider.ActivityProvider
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Returns current system inset values so web content can adapt its layout.
 *
 * Web usage:
 * ```javascript
 * const insets = await jsbridge.call({ data: { action: 'getInsets' } });
 * ```
 */
class GetInsetsCommand : BridgeCommand {

    override val action = "getInsets"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val activity = ActivityProvider.currentActivity
                ?: return@withContext BridgeResponseUtils.createErrorResponse(
                    "NO_ACTIVITY",
                    "No active activity"
                )

            val window = activity.window
            val decorView = window.decorView
            val rootInsets = decorView.rootWindowInsets

            val result = JSONObject()

            if (rootInsets != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val statusBars = rootInsets.getInsets(WindowInsets.Type.statusBars())
                val navBars = rootInsets.getInsets(WindowInsets.Type.navigationBars())
                val ime = rootInsets.getInsets(WindowInsets.Type.ime())

                result.put("statusBar", JSONObject().apply {
                    put("height", statusBars.top)
                    put("visible", rootInsets.isVisible(WindowInsets.Type.statusBars()))
                })
                result.put("systemNavigation", JSONObject().apply {
                    put("height", navBars.bottom)
                    put("visible", rootInsets.isVisible(WindowInsets.Type.navigationBars()))
                })
                result.put("keyboard", JSONObject().apply {
                    put("height", ime.bottom)
                    put("visible", rootInsets.isVisible(WindowInsets.Type.ime()))
                })
                result.put("safeArea", JSONObject().apply {
                    put("top", statusBars.top)
                    put("right", navBars.right)
                    put("bottom", navBars.bottom)
                    put("left", navBars.left)
                })
            } else {
                @Suppress("DEPRECATION")
                val insets = rootInsets
                val statusBarHeight = insets?.systemWindowInsetTop ?: 0
                val navBarHeight = insets?.systemWindowInsetBottom ?: 0

                result.put("statusBar", JSONObject().apply {
                    put("height", statusBarHeight)
                    put("visible", true)
                })
                result.put("systemNavigation", JSONObject().apply {
                    put("height", navBarHeight)
                    put("visible", true)
                })
                result.put("keyboard", JSONObject().apply {
                    put("height", 0)
                    put("visible", false)
                })
                result.put("safeArea", JSONObject().apply {
                    put("top", statusBarHeight)
                    put("right", 0)
                    put("bottom", navBarHeight)
                    put("left", 0)
                })
            }

            Timber.i("[handle] insets=$result")
            result
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse("INSETS_FAILED", e.message ?: "Failed to get insets")
        }
    }
}
