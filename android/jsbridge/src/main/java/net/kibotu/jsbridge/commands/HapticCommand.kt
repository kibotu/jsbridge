package net.kibotu.jsbridge.commands

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import net.kibotu.jsbridge.commands.utils.BridgeParsingUtils
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import com.github.florent37.application.provider.ActivityProvider
import com.github.florent37.application.provider.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

class HapticCommand : BridgeCommand {

    override val action = "haptic"

    override suspend fun handle(content: Any?): JSONObject =
        withContext(Dispatchers.Main) {
            try {
                val vibrate = BridgeParsingUtils.parseBoolean(content, "vibrate")

                Timber.i("[handle] vibrate=$vibrate")

                if (vibrate == false) {
                    return@withContext BridgeResponseUtils.createSuccessResponse()
                }

                val context = requireNotNull(ActivityProvider.currentActivity ?: application)
                val vibrator = getVibrator(context)
                    ?: return@withContext BridgeResponseUtils.createErrorResponse(
                        "VIBRATOR_UNAVAILABLE",
                        "Vibrator service not available"
                    )

                if (!vibrator.hasVibrator()) {
                    return@withContext BridgeResponseUtils.createErrorResponse(
                        "VIBRATOR_UNAVAILABLE",
                        "Device does not have a vibrator"
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
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

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

