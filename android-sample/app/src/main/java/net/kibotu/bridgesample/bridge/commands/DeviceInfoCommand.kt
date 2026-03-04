package net.kibotu.bridgesample.bridge.commands

import android.os.Build
import com.github.florent37.application.provider.ActivityProvider
import com.github.florent37.application.provider.application
import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import org.json.JSONObject

/**
 * Provides device and app metadata to web for adaptive UI and analytics.
 *
 * **Why web needs this:**
 * Web code can't access device info directly (security sandbox). Needed for:
 * - Responsive UI adaptation (tablet vs phone layouts)
 * - Bug reports with device context
 * - Feature detection (OS-specific capabilities)
 * - Analytics and crash reporting enrichment
 *
 * **Why this data:**
 * - Platform: Always "Android" for web to detect OS
 * - OS/SDK versions: Feature detection (API 30+ capabilities)
 * - Manufacturer/Model: Known device-specific bugs/limitations
 * - App version: Backend compatibility and feature flags
 */
class DeviceInfoCommand : BridgeCommand {

    override val action = "deviceInfo"

    override suspend fun handle(content: Any?): Any? {
        val context = requireNotNull(ActivityProvider.currentActivity ?: application)
        return JSONObject().apply {
            put("platform", "Android")
            put("osVersion", Build.VERSION.RELEASE)
            put("sdkVersion", Build.VERSION.SDK_INT)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put(
                "appVersion", try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) {
                    "unknown"
                }
            )
        }
    }
}

