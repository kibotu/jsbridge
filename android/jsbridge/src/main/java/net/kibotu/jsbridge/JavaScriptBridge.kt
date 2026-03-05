package net.kibotu.jsbridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.kibotu.jsbridge.commands.BridgeCommand
import net.kibotu.jsbridge.commands.utils.BridgeResponseUtils
import net.kibotu.jsbridge.decorators.BridgeWebViewClient
import org.json.JSONObject
import timber.log.Timber
import java.util.WeakHashMap

/**
 * Enables seamless bidirectional communication between web content and native Android.
 *
 * Web content interacts via `window.<bridgeName>` (default: `window.jsbridge`).
 * The bridge name is configurable so multiple bridges can coexist on the same WebView,
 * each with its own set of commands.
 *
 * @param webView The WebView instance to bridge with
 * @param commands List of command handlers this bridge responds to
 * @param bridgeName Name exposed to JavaScript (default [DEFAULT_BRIDGE_NAME])
 * @see <a href="https://github.com/kibotu/jsbridge">jsbridge specification</a>
 */
class JavaScriptBridge(
    private val webView: WebView,
    private val commands: List<BridgeCommand>,
    private val bridgeName: String = DEFAULT_BRIDGE_NAME
) {
    /** The WebView's context, used by commands and services to resolve Activity/Application. */
    val context get() = webView.context

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val callbackResponse get() = "__${bridgeName}_handleResponse"
    private val callbackMessage get() = "__${bridgeName}_handleNativeMessage"

    companion object {
        const val DEFAULT_BRIDGE_NAME = "jsbridge"
        const val SCHEMA_VERSION = 1

        private val bridges = WeakHashMap<WebView, MutableMap<String, JavaScriptBridge>>()

        /**
         * Retrieve the bridge associated with a WebView by name.
         * Falls back to the default bridge name if not specified.
         */
        fun WebView.bridge(name: String = DEFAULT_BRIDGE_NAME): JavaScriptBridge? =
            bridges[this]?.get(name)

        /**
         * One-liner to set up the JavaScript bridge on a [WebView].
         *
         * - Creates the bridge and registers the `@JavascriptInterface`
         * - Wraps the existing [android.webkit.WebViewClient] with [BridgeWebViewClient]
         *   (decorator pattern) so bridge injection and safe area CSS happen automatically
         * - Associates the bridge with the WebView for retrieval via [bridge]
         *
         * ```kotlin
         * val bridge = JavaScriptBridge.inject(
         *     webView = webView,
         *     commands = listOf(DeviceInfoCommand(), ShowToastCommand()),
         *     bridgeName = "jsbridge"
         * )
         * ```
         *
         * @return The created [JavaScriptBridge] instance.
         */
        fun inject(
            webView: WebView,
            commands: List<BridgeCommand>,
            bridgeName: String = DEFAULT_BRIDGE_NAME
        ): JavaScriptBridge {
            val bridge = JavaScriptBridge(webView, commands, bridgeName)
            webView.addJavascriptInterface(bridge, bridgeName)

            val map = bridges.getOrPut(webView) { mutableMapOf() }
            map[bridgeName] = bridge

            val currentClient = webView.webViewClient
            if (currentClient !is BridgeWebViewClient) {
                webView.webViewClient = BridgeWebViewClient(currentClient)
            }

            Timber.d("[Bridge] Injected (name=$bridgeName, commands=${commands.size})")
            return bridge
        }
    }

    /**
     * Entry point for all web-to-native communication.
     *
     * @param message JSON string from web: `{ id?, version?, data: { action, content? } }`
     */
    @Suppress("unused")
    @JavascriptInterface
    fun postMessage(message: String) {
        Timber.d("[postMessage] received: $message")

        scope.launch {
            var id: String? = null
            try {
                val messageObj = JSONObject(message)
                id = messageObj.optString("id", null)
                val data = messageObj.optJSONObject("data")

                if (data == null) {
                    sendErrorToWeb(id, "INVALID_MESSAGE", "Missing 'data' field in message")
                    return@launch
                }

                val requestedVersion = messageObj.optInt("version", SCHEMA_VERSION)
                if (requestedVersion > SCHEMA_VERSION) {
                    Timber.w("[postMessage] Ignoring message with version $requestedVersion (current: $SCHEMA_VERSION)")
                    return@launch
                }

                val action = data.optString("action", null)
                if (action.isNullOrEmpty()) {
                    sendErrorToWeb(id, "INVALID_MESSAGE", "Missing 'action' field in data")
                    return@launch
                }

                val content = data.opt("content")
                val command = commands.find { it.action == action }

                if (command == null) {
                    Timber.w("[postMessage] Unknown action: $action")
                    sendErrorToWeb(id, "UNKNOWN_ACTION", "Unknown action: $action")
                    return@launch
                }

                val result = command.handle(content)

                if (!id.isNullOrEmpty()) {
                    sendResponseToWeb(id, result)
                }
            } catch (e: Exception) {
                Timber.e(e)
                sendErrorToWeb(id, "INTERNAL_ERROR", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Cancels all in-flight coroutines and removes the bridge association.
     * Call when the hosting component (Fragment, Activity) is destroyed.
     */
    fun destroy() {
        scope.cancel()
        bridges[webView]?.remove(bridgeName)
        if (bridges[webView]?.isEmpty() == true) {
            bridges.remove(webView)
        }
        Timber.d("[Bridge] Destroyed (name=$bridgeName)")
    }

    /**
     * Send a message from native to web (push notifications, state updates, events).
     */
    fun sendToWeb(action: String, content: Any? = null) {
        scope.launch {
            try {
                val message = buildNativeMessage(action, content)
                val script = "window.$callbackMessage && window.$callbackMessage($message)"
                executeJavaScript(script)
                Timber.d("[sendToWeb] action=$action content=$content")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun sendResponseToWeb(id: String, result: Any?) {
        if (result is JSONObject && result.has("error")) {
            val error = result.optJSONObject("error")
            if (error != null) {
                sendErrorToWeb(id, error.optString("code", "UNKNOWN"), error.optString("message", "Unknown error"))
                return
            }
        }

        val response = JSONObject().apply {
            put("id", id)
            put("data", result)
        }

        val script = "window.$callbackResponse && window.$callbackResponse($response)"
        executeJavaScript(script)

        Timber.d("[sendResponseToWeb] id=$id result=$result")
    }

    private fun sendErrorToWeb(id: String?, code: String, message: String) {
        val error = JSONObject().apply {
            put("code", code)
            put("message", message)
        }

        val response = JSONObject().apply {
            if (!id.isNullOrEmpty()) put("id", id)
            put("error", error)
        }

        val script = "window.$callbackResponse && window.$callbackResponse($response)"
        executeJavaScript(script)

        Timber.w("[sendErrorToWeb] id=$id code=$code message=$message")
    }

    private fun buildNativeMessage(action: String, content: Any?): String {
        val message = JSONObject()
        val data = JSONObject().apply {
            put("action", action)
            if (content != null) {
                val jsonContent = when (content) {
                    is Map<*, *> -> JSONObject(content)
                    else -> content
                }
                put("content", jsonContent)
            }
        }
        message.put("data", data)
        return message.toString()
    }

    private fun executeJavaScript(script: String) {
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Inject safe area CSS custom properties into the WebView.
     * Call this whenever bars change visibility, on rotation, or on keyboard show/hide.
     */
    fun updateSafeAreaCSS(
        insetTop: Int = 0,
        insetBottom: Int = 0,
        insetLeft: Int = 0,
        insetRight: Int = 0,
        statusBarHeight: Int = 0,
        topNavHeight: Int = 0,
        bottomNavHeight: Int = 0,
        systemNavHeight: Int = 0
    ) {
        val js = """
            (function() {
                var el = document.documentElement;
                el.style.setProperty('--bridge-inset-top', '${insetTop}px');
                el.style.setProperty('--bridge-inset-bottom', '${insetBottom}px');
                el.style.setProperty('--bridge-inset-left', '${insetLeft}px');
                el.style.setProperty('--bridge-inset-right', '${insetRight}px');
                el.style.setProperty('--bridge-status-bar', '${statusBarHeight}px');
                el.style.setProperty('--bridge-top-nav', '${topNavHeight}px');
                el.style.setProperty('--bridge-bottom-nav', '${bottomNavHeight}px');
                el.style.setProperty('--bridge-system-nav', '${systemNavHeight}px');
            })();
        """.trimIndent()
        executeJavaScript(js)
    }

    /**
     * Injects the unified bridge JavaScript API into the WebView.
     * Reads from res/raw/bridge.js and replaces template variables.
     * Call on page load and after navigation.
     */
    fun injectBridgeScript() {
        val script = loadBridgeScript()
        executeJavaScript(script)
        Timber.d("[injectBridgeScript] Bridge script injected (bridgeName=$bridgeName)")
    }

    private fun loadBridgeScript(): String {
        val rawScript = webView.context.resources
            .openRawResource(R.raw.bridge)
            .bufferedReader()
            .use { it.readText() }

        return rawScript
            .replace("__BRIDGE_NAME__", bridgeName)
            .replace("__SCHEMA_VERSION__", SCHEMA_VERSION.toString())
    }
}
