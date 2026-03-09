package net.kibotu.jsbridge

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber

/**
 * Encrypted key-value storage with automatic backend selection.
 *
 * Prefers Google Tink (AES-256-GCM via Android Keystore) when available on the
 * classpath, otherwise falls back to the legacy EncryptedSharedPreferences.
 *
 * The consuming app controls which backend is active by declaring the
 * appropriate runtime dependency:
 * - `implementation("com.google.crypto.tink:tink-android:1.20.0")`  -- Tink
 * - `implementation("androidx.security:security-crypto:1.1.0")`     -- legacy fallback
 */
class SecureStorage(context: Context) {

    private val delegate: SecureStorageBackend = createBackend(context)

    fun save(key: String, value: String) = delegate.save(key, value)
    fun load(key: String): String? = delegate.load(key)
    fun remove(key: String) = delegate.remove(key)
    fun contains(key: String): Boolean = delegate.contains(key)

    companion object {
        internal const val PREF_DATA_FILE = "secure_storage"

        private fun createBackend(context: Context): SecureStorageBackend {
            if (isTinkAvailable()) {
                Timber.d("[SecureStorage] Using Tink backend")
                return TinkSecureStorageBackend(context)
            }
            if (isEncryptedSharedPreferencesAvailable()) {
                Timber.d("[SecureStorage] Tink not found, falling back to EncryptedSharedPreferences")
                return LegacySecureStorageBackend(context)
            }
            error(
                "No encryption backend available. " +
                    "Add com.google.crypto.tink:tink-android or " +
                    "androidx.security:security-crypto to your dependencies."
            )
        }

        private fun isTinkAvailable(): Boolean = try {
            Class.forName("com.google.crypto.tink.Aead")
            true
        } catch (_: ClassNotFoundException) {
            false
        }

        private fun isEncryptedSharedPreferencesAvailable(): Boolean = try {
            Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}

internal interface SecureStorageBackend {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)
    fun contains(key: String): Boolean
}

internal class TinkSecureStorageBackend(context: Context) : SecureStorageBackend {

    private val aead: com.google.crypto.tink.Aead
    private val prefs: SharedPreferences

    init {
        com.google.crypto.tink.aead.AeadConfig.register()

        val keysetHandle = com.google.crypto.tink.integration.android.AndroidKeysetManager.Builder()
            .withKeyTemplate(com.google.crypto.tink.KeyTemplates.get("AES256_GCM"))
            .withSharedPref(context, KEYSET_NAME, PREF_KEYSET_FILE)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(
            com.google.crypto.tink.RegistryConfiguration.get(),
            com.google.crypto.tink.Aead::class.java
        )
        prefs = context.getSharedPreferences(SecureStorage.PREF_DATA_FILE, Context.MODE_PRIVATE)
    }

    override fun save(key: String, value: String) {
        val encrypted = aead.encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        prefs.edit { putString(key, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)) }
    }

    override fun load(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val decrypted = aead.decrypt(encrypted, key.toByteArray(Charsets.UTF_8))
        return String(decrypted, Charsets.UTF_8)
    }

    override fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    companion object {
        private const val KEYSET_NAME = "bridge_secure_keyset"
        private const val PREF_KEYSET_FILE = "bridge_secure_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://bridge_tink_master_key"
    }
}

@Suppress("DEPRECATION")
internal class LegacySecureStorageBackend(context: Context) : SecureStorageBackend {

    private val prefs: SharedPreferences

    init {
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            SecureStorage.PREF_DATA_FILE,
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun save(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override fun load(key: String): String? = prefs.getString(key, null)

    override fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    override fun contains(key: String): Boolean = prefs.contains(key)
}
