package net.kibotu.jsbridge.commands

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kibotu.jsbridge.BridgeContextProvider
import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import org.json.JSONObject
import timber.log.Timber

/**
 * Triggers haptic feedback so web content can provide tactile responses to user actions.
 *
 * Strategy per API level:
 *
 * **API < 33:** [android.view.View.performHapticFeedback] with the deprecated
 * [HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING] flag, which forces feedback
 * even when the user has disabled system haptics.
 *
 * **API 33+:** [VibrationEffect.createOneShot] via [Vibrator] with
 * [VibrationAttributes.USAGE_HARDWARE_FEEDBACK]. We avoid [USAGE_TOUCH] because
 * the system suppresses it when the user's "Touch feedback" intensity is OFF
 * (controlled by [Settings.System.HAPTIC_FEEDBACK_INTENSITY]). We also avoid
 * [performHapticFeedback] because on API 33+ it can return `true` while producing
 * no vibration. [USAGE_HARDWARE_FEEDBACK] maps to
 * [Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY], which the Settings app
 * keeps at the device default even when the user disables touch haptics.
 */
class HapticCommand(private val contextProvider: () -> Context?) : BridgeCommand {

    override val action = "haptic"

    override suspend fun handle(content: Any?): JSONObject =
        withContext(Dispatchers.Main) {
            try {
                val vibrate = BridgeParsingUtils.parseBoolean(content, "vibrate")

                Timber.i("[HapticCommand] vibrate=$vibrate")

                if (vibrate == false) {
                    return@withContext BridgeResponseUtils.createSuccessResponse()
                }

                val context = requireNotNull(
                    BridgeContextProvider.findActivity(contextProvider()) ?: contextProvider()
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    performHapticApi33(context)
                } else {
                    performHapticLegacy(context)
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun performHapticApi33(context: Context) {
        val vibrator = getVibrator(context)

        if (vibrator == null || !vibrator.hasVibrator()) {
            Timber.w("[HapticCommand] No vibrator available")
            return
        }

        val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        val attrs = VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
            .build()
        vibrator.vibrate(effect, attrs)
    }

    /**
     * API < 33: Uses [performHapticFeedback] with deprecated flags to ignore both
     * view-level and global haptic settings. Falls back to [Vibrator] if no
     * Activity/decorView is available.
     */
    private fun performHapticLegacy(context: Context) {
        val activity = context as? Activity ?: BridgeContextProvider.findActivity(context)
        val view = activity?.window?.decorView

        if (view != null) {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }

            @Suppress("DEPRECATION")
            view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            return
        }

        Timber.d("[HapticCommand] No decor view, falling back to Vibrator")
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

