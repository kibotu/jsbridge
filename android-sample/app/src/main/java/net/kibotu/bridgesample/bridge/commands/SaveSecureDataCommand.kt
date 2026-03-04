package net.kibotu.bridgesample.bridge.commands

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
 * Persists sensitive data to Android's encrypted storage.
 *
 * **Why web needs this:**
 * Web's localStorage/sessionStorage are NOT secure:
 * - Stored as plaintext (accessible via adb, root, file explorers)
 * - Can be read by malicious JavaScript
 * - Not encrypted at rest
 * Native encrypted storage protects sensitive data using Android Keystore.
 *
 * **Why encrypted storage:**
 * Critical for storing:
 * - Authentication tokens/refresh tokens
 * - User credentials (if needed)
 * - API keys
 * - PII that should never be in plaintext
 *
 * **Why Dispatchers.IO:**
 * Storage operations are I/O bound. IO dispatcher optimized for blocking
 * operations, prevents blocking main thread during disk writes.
 *
 * **Why key-value model:**
 * Simple, familiar pattern (like localStorage API). Web developers understand
 * key-value storage immediately, reducing learning curve.
 *
 * **Implementation placeholder:**
 * Awaits integration with actual EncryptedSharedPreferences or Keystore-based
 * storage solution. Currently logs for development/debugging.
 */
class SaveSecureDataCommand : BridgeCommand {

    override val action = "saveSecureData"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.IO) {
        val key = BridgeParsingUtils.parseString(content, "key")
        val value = BridgeParsingUtils.parseString(content, "value")

        if (key.isEmpty()) {
            return@withContext BridgeResponseUtils.createErrorResponse(
                "INVALID_PARAMETER",
                "Missing 'key' parameter"
            )
        }

        try {
            val context = ActivityProvider.currentActivity ?: application

            // Create or retrieve a strong MasterKey backed by Android Keystore
            val masterKey = MasterKey.Builder(requireNotNull(context))
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // Encrypted SharedPreferences instance
            val prefs = EncryptedSharedPreferences.create(
                requireNotNull(context),
                "secure_storage",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.edit().putString(key, value).apply()
            Timber.i("[handle] saved key=$key, valueLength=${value.length}")
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "SAVE_FAILED",
                e.message ?: "Failed to save data"
            )
        }
    }
}

