package net.kibotu.bridgesample.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.kibotu.bridgesample.bridge.decorators.BridgeWebViewClient
import org.json.JSONObject
import timber.log.Timber
import java.util.WeakHashMap

/**
 * Enables seamless bidirectional communication between web content and native Android.
 *
 * Web content interacts via `window.<bridgeName>` (default: `window.jsbridge`).
 * The bridge name is configurable so each app can use its own namespace.
 *
 * Based on check-mate specification for standardized web-native communication.
 *
 * @param webView The WebView instance to bridge with
 * @param messageHandler Handles routing and execution of bridge commands
 * @param bridgeName Name exposed to JavaScript (default [DEFAULT_BRIDGE_NAME])
 * @see <a href="https://github.com/kibotu/check-mate">check-mate specification</a>
 */
class JavaScriptBridge(
    private val webView: WebView,
    private val messageHandler: BridgeMessageHandler,
    private val bridgeName: String = DEFAULT_BRIDGE_NAME
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val callbackResponse get() = "__${bridgeName}_handleResponse"
    private val callbackMessage get() = "__${bridgeName}_handleNativeMessage"

    companion object {
        const val DEFAULT_BRIDGE_NAME = "jsbridge"
        const val SCHEMA_VERSION = 1

        private val bridges = WeakHashMap<WebView, JavaScriptBridge>()

        /**
         * Extension to retrieve the bridge associated with a WebView.
         */
        @Suppress("unused")
        val WebView.bridge: JavaScriptBridge?
            get() = bridges[this]

        /**
         * One-liner to set up the JavaScript bridge on a [WebView].
         *
         * - Creates the bridge and registers the `@JavascriptInterface`
         * - Wraps the existing [android.webkit.WebViewClient] with [BridgeWebViewClient]
         *   (decorator pattern) so bridge injection and safe area CSS happen automatically
         * - Associates the bridge with the WebView for retrieval
         *
         * ```kotlin
         * // In a Fragment / Compose factory — that's it:
         * val bridge = JavaScriptBridge.inject(webView, DefaultBridgeMessageHandler())
         * ```
         *
         * @return The created [JavaScriptBridge] instance.
         */
        fun inject(
            webView: WebView,
            messageHandler: BridgeMessageHandler,
            bridgeName: String = DEFAULT_BRIDGE_NAME
        ): JavaScriptBridge {
            val bridge = JavaScriptBridge(webView, messageHandler, bridgeName)
            webView.addJavascriptInterface(bridge, bridgeName)
            bridges[webView] = bridge

            val currentClient = webView.webViewClient
            webView.webViewClient = BridgeWebViewClient(currentClient)

            Timber.d("[Bridge] Injected (name=$bridgeName)")
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
                val result = messageHandler.handle(action, content)

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
        bridges.remove(webView)
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
     * Call on page load and after navigation.
     */
    fun injectBridgeScript() {
        val script = getBridgeJavaScript()
        executeJavaScript(script)
        Timber.d("[injectBridgeScript] Bridge script injected (bridgeName=$bridgeName)")
    }

    private fun getBridgeJavaScript(): String {
        return """
(function () {
  'use strict';

  try {
    (function initializeBridge() {
      var BRIDGE_NAME = '$bridgeName';
      var SCHEMA_VERSION = $SCHEMA_VERSION;
      var DEFAULT_TIMEOUT = 30000;
      var CALLBACK_RESPONSE = '__' + BRIDGE_NAME + '_handleResponse';
      var CALLBACK_MESSAGE = '__' + BRIDGE_NAME + '_handleNativeMessage';

      var nativeAndroid = (window[BRIDGE_NAME] && typeof window[BRIDGE_NAME].postMessage === 'function')
        ? window[BRIDGE_NAME]
        : null;

      var nativeIOS = (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers[BRIDGE_NAME])
        ? window.webkit.messageHandlers[BRIDGE_NAME]
        : null;

      if (window[BRIDGE_NAME] && window[BRIDGE_NAME].schemaVersion != null) {
        console.warn('[' + BRIDGE_NAME + '] Already initialized');
        return;
      }

      var initStart = performance.now();
      var debug = false;
      var pendingPromises = {};
      var messageHandlers = [];
      var mockHandler = null;
      var idCounter = 0;
      var _platform = nativeAndroid ? 'android' : (nativeIOS ? 'ios' : 'desktop');

      var readyResolve;
      var readyPromise = new Promise(function (resolve) { readyResolve = resolve; });

      function debugLog() {
        if (debug) {
          var args = ['[' + BRIDGE_NAME + ']'];
          for (var i = 0; i < arguments.length; i++) args.push(arguments[i]);
          console.log.apply(console, args);
        }
      }

      function generateId() {
        return 'msg_' + Date.now() + '_' + (++idCounter) + '_' + Math.random().toString(36).substring(2, 11);
      }

      function validateMessage(message) {
        if (!message || typeof message !== 'object') throw new Error('Message must be an object');
        if (!message.data || typeof message.data !== 'object') throw new Error('Message must contain a data object');
        if (!message.data.action || typeof message.data.action !== 'string') throw new Error('Message data must contain an action string');
      }

      function sendToNative(message) {
        var str = JSON.stringify(message);
        if (nativeAndroid) { nativeAndroid.postMessage(str); return; }
        if (nativeIOS) { nativeIOS.postMessage(str); return; }
        if (mockHandler) {
          try {
            var result = mockHandler(message);
            if (result && typeof result.then === 'function') {
              result.then(function (d) { handleResponse({ id: message.id, data: d }); })
                    .catch(function (e) { handleResponse({ id: message.id, error: { code: 'MOCK_ERROR', message: e.message || String(e) } }); });
            } else {
              setTimeout(function () { handleResponse({ id: message.id, data: result }); }, 0);
            }
          } catch (e) {
            setTimeout(function () { handleResponse({ id: message.id, error: { code: 'MOCK_ERROR', message: e.message || String(e) } }); }, 0);
          }
          return;
        }
        throw new Error('Native bridge not available. Use ' + BRIDGE_NAME + '.setMockHandler(fn) for desktop testing.');
      }

      function cleanupPromise(id) {
        var entry = pendingPromises[id];
        if (entry && entry.timeoutId) clearTimeout(entry.timeoutId);
        delete pendingPromises[id];
      }

      function handleResponse(response) {
        debugLog('Received response:', response);
        if (!response || typeof response !== 'object' || !response.id) { console.error('[' + BRIDGE_NAME + '] Invalid response:', response); return; }
        var entry = pendingPromises[response.id];
        if (!entry) { debugLog('No pending promise for id: ' + response.id); return; }
        cleanupPromise(response.id);
        if (response.error) {
          var err = new Error(response.error.message || 'Unknown error');
          err.code = response.error.code || 'UNKNOWN';
          entry.reject(err);
        } else {
          entry.resolve(response.data != null ? response.data : {});
        }
      }

      function handleNativeMessage(message) {
        debugLog('Received native message:', message);
        if (messageHandlers.length === 0) { debugLog('No message handlers registered'); return; }
        for (var i = 0; i < messageHandlers.length; i++) {
          try { messageHandlers[i](message); } catch (e) { console.error('[' + BRIDGE_NAME + '] Handler error:', e); }
        }
      }

      var bridge = {
        schemaVersion: SCHEMA_VERSION,
        ready: function () { return readyPromise; },
        setDebug: function (enabled) { debug = Boolean(enabled); debugLog('Debug ' + (debug ? 'enabled' : 'disabled')); },
        call: function (actionOrMessage, contentOrOptions, options) {
          var message; var opts;
          if (typeof actionOrMessage === 'string') {
            message = { data: { action: actionOrMessage } };
            if (contentOrOptions != null) message.data.content = contentOrOptions;
            opts = options || {};
          } else { message = actionOrMessage; opts = contentOrOptions || {}; }
          return new Promise(function (resolve, reject) {
            var id;
            try {
              validateMessage(message);
              id = generateId();
              var timeout = opts.timeout != null ? opts.timeout : DEFAULT_TIMEOUT;
              var version = opts.version != null ? opts.version : SCHEMA_VERSION;
              var fullMessage = { version: version, id: id, data: message.data };
              debugLog('Calling native:', fullMessage);
              var timeoutId = setTimeout(function () { cleanupPromise(id); reject(new Error('Request timeout after ' + timeout + 'ms')); }, timeout);
              pendingPromises[id] = { resolve: resolve, reject: reject, timeoutId: timeoutId };
              sendToNative(fullMessage);
            } catch (e) { if (id) cleanupPromise(id); debugLog('Call failed:', e); reject(e); }
          });
        },
        on: function (handler) {
          if (typeof handler !== 'function') throw new Error('Handler must be a function');
          messageHandlers.push(handler);
          debugLog('Handler registered (' + messageHandlers.length + ' total)');
        },
        off: function (handler) {
          var idx = messageHandlers.indexOf(handler);
          if (idx !== -1) { messageHandlers.splice(idx, 1); debugLog('Handler removed (' + messageHandlers.length + ' remaining)'); }
        },
        setMockHandler: function (handler) {
          if (handler !== null && typeof handler !== 'function') throw new Error('Mock handler must be a function or null');
          mockHandler = handler;
          if (handler) { _platform = 'desktop'; debugLog('Mock handler set'); }
        },
        cancelAll: function () {
          var ids = Object.keys(pendingPromises);
          var err = new Error('All pending requests cancelled');
          err.code = 'CANCELLED';
          for (var i = 0; i < ids.length; i++) { var entry = pendingPromises[ids[i]]; cleanupPromise(ids[i]); entry.reject(err); }
          debugLog('Cancelled ' + ids.length + ' pending request(s)');
        },
        getStats: function () {
          return { pendingRequests: Object.keys(pendingPromises).length, schemaVersion: SCHEMA_VERSION, platform: _platform, handlers: messageHandlers.length, debugEnabled: debug };
        }
      };

      Object.defineProperty(bridge, 'platform', { get: function () { return _platform; }, enumerable: true });

      window[CALLBACK_RESPONSE] = handleResponse;
      window[CALLBACK_MESSAGE] = handleNativeMessage;

      Object.defineProperty(window, BRIDGE_NAME, { value: Object.freeze(bridge), writable: false, configurable: false });

      setTimeout(function () {
        var ms = (performance.now() - initStart).toFixed(2);
        readyResolve();
        try { window.dispatchEvent(new CustomEvent('bridgeReady', { detail: { schemaVersion: SCHEMA_VERSION, platform: _platform } })); } catch (e) {}
        console.log('[' + BRIDGE_NAME + '] Initialized (v' + SCHEMA_VERSION + ', ' + _platform + ') in ' + ms + 'ms');
      }, 0);
    })();
  } catch (e) {
    console.error('[Bridge] Init failed:', e);
  }
})();
"""
    }
}
