package net.kibotu.jsbridge.commands

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import net.kibotu.jsbridge.BridgeContextProvider
import org.json.JSONObject

/**
 * Reports real network connectivity status to web for offline-first UX.
 *
 * **Why web needs this:**
 * Web's `navigator.onLine` is unreliable (reports false positives). Native Android
 * has accurate network state through ConnectivityManager. Enables web to:
 * - Show offline UI before failed requests
 * - Queue actions for later when offline
 * - Disable network-dependent features gracefully
 * - Adapt quality (low bandwidth = reduced images/video)
 *
 * **Why type field:**
 * Future extensibility for network type (wifi/cellular/ethernet) to enable
 * bandwidth-aware UX decisions.
 */
class NetworkStatusCommand(private val contextProvider: () -> Context?) : BridgeCommand {

    override val action = "networkState"

    override suspend fun handle(content: Any?): JSONObject {
        val context = BridgeContextProvider.findActivity(contextProvider()) ?: contextProvider()
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        var isConnected = false
        var connectionType = "none"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        isConnected = true
                        connectionType = when {
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
                            else -> "unknown"
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                isConnected = activeNetworkInfo?.isConnectedOrConnecting == true
                @Suppress("DEPRECATION")
                connectionType = when (activeNetworkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "wifi"
                    ConnectivityManager.TYPE_MOBILE -> "cellular"
                    ConnectivityManager.TYPE_ETHERNET -> "ethernet"
                    else -> if (isConnected) "unknown" else "none"
                }
            }
        } catch (_: Exception) {
            isConnected = false
            connectionType = "none"
        }

        return JSONObject().apply {
            put("connected", isConnected)
            put("type", connectionType)
        }
    }
}

