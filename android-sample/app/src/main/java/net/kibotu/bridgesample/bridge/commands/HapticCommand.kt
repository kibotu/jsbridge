package net.kibotu.bridgesample.bridge.commands

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import net.kibotu.bridgesample.bridge.commands.utils.BridgeParsingUtils
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import com.github.florent37.application.provider.application
import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Triggers haptic feedback for tactile user interaction feedback.
 *
 * **Why web needs this:**
 * Web's Vibration API is limited/unsupported on many devices. Native haptics provide:
 * - Confirmation feedback for successful actions (button taps, form submissions)
 * - Attention-grabbing for important events (errors, notifications)
 * - Enhanced UX making app feel more responsive and "native"
 *
 * **Why boolean control:**
 * Simple on/off is sufficient for 90% of use cases. Complex vibration patterns
 * can be added later if needed (vibration patterns API).
 *
 * **Why 50ms duration:**
 * Tested sweet spot - noticeable but not annoying. Short enough for quick
 * succession (multiple taps) without overlap discomfort.
 *
 * **Why API level check:**
 * VibrationEffect API was added in API 26 (Oreo). Fallback to deprecated
 * method ensures compatibility with older devices.
 */
class HapticCommand : BridgeCommand {

    override val action = "haptic"

    override suspend fun handle(content: Any?): JSONObject =
        withContext(Dispatchers.Main) @androidx.annotation.RequiresPermission(
            android.Manifest.permission.VIBRATE
        ) {
            try {
                val vibrate = BridgeParsingUtils.parseBoolean(content, "vibrate")

                Timber.i("[handle] vibrate=$vibrate")

                if (vibrate == false) {
                    return@withContext BridgeResponseUtils.createSuccessResponse()
                }

                val context = requireNotNull(ActivityProvider.currentActivity ?: application)
                val vibrator = context.getSystemService<Vibrator>()
                    ?: return@withContext BridgeResponseUtils.createErrorResponse(
                        "VIBRATOR_UNAVAILABLE",
                        "Vibrator service not available"
                    )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }

                BridgeResponseUtils.createSuccessResponse()
            } catch (e: Exception) {
                Timber.e(e)
                BridgeResponseUtils.createErrorResponse(
                    "HAPTIC_FAILED",
                    e.message ?: "Failed to trigger haptic feedback"
                )
            }
        }
}

