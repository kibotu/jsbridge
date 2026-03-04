import Foundation

/// Single source of truth for the injected JavaScript bridge code.
///
/// Both Android and iOS use an identical JS payload. The only difference is
/// how it reaches the WebView (Android: `evaluateJavascript`, iOS: `WKUserScript`).
///
/// Template variables replaced at injection time:
/// - `__BRIDGE_NAME__`    → the configured bridge name (e.g. `"jsbridge"`)
/// - `__SCHEMA_VERSION__` → integer schema version
struct JavaScriptBridgeScript {

    /// Returns the unified bridge JS with name and version templated in.
    static func source(bridgeName: String = "jsbridge", schemaVersion: Int = 1, debug: Bool = false) -> String {
        return bridgeTemplate
            .replacingOccurrences(of: "__BRIDGE_NAME__", with: bridgeName)
            .replacingOccurrences(of: "__SCHEMA_VERSION__", with: String(schemaVersion))
    }

    // The unified bridge script -- identical to bridge.js at the repo root
    // and to the string embedded in Android's JavaScriptBridge.kt.
    private static let bridgeTemplate = """
(function () {
  'use strict';

  try {
    (function initializeBridge() {
      var BRIDGE_NAME = '__BRIDGE_NAME__';
      var SCHEMA_VERSION = __SCHEMA_VERSION__;
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
      var platform = nativeAndroid ? 'android' : (nativeIOS ? 'ios' : 'desktop');

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
        return 'msg_' + Date.now() + '_' + (++idCounter) + '_' + Math.random().toString(36).substr(2, 9);
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
        platform: platform,
        ready: function () { return readyPromise; },
        setDebug: function (enabled) { debug = Boolean(enabled); debugLog('Debug ' + (debug ? 'enabled' : 'disabled')); },
        call: function (message, options) {
          options = options || {};
          return new Promise(function (resolve, reject) {
            try {
              validateMessage(message);
              var id = generateId();
              var timeout = options.timeout != null ? options.timeout : DEFAULT_TIMEOUT;
              var fullMessage = { version: SCHEMA_VERSION, id: id, data: message.data };
              debugLog('Calling native:', fullMessage);
              var timeoutId = setTimeout(function () { cleanupPromise(id); reject(new Error('Request timeout after ' + timeout + 'ms')); }, timeout);
              pendingPromises[id] = { resolve: resolve, reject: reject, timeoutId: timeoutId };
              sendToNative(fullMessage);
            } catch (e) { debugLog('Call failed:', e); reject(e); }
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
          if (handler) { platform = 'desktop'; debugLog('Mock handler set'); }
        },
        getStats: function () {
          return { pendingRequests: Object.keys(pendingPromises).length, schemaVersion: SCHEMA_VERSION, platform: platform, handlers: messageHandlers.length, debugEnabled: debug };
        }
      };

      window[CALLBACK_RESPONSE] = handleResponse;
      window[CALLBACK_MESSAGE] = handleNativeMessage;

      Object.defineProperty(window, BRIDGE_NAME, { value: Object.freeze(bridge), writable: false, configurable: false });

      setTimeout(function () {
        var ms = (performance.now() - initStart).toFixed(2);
        readyResolve();
        try { window.dispatchEvent(new CustomEvent('bridgeReady', { detail: { schemaVersion: SCHEMA_VERSION, platform: platform } })); } catch (e) {}
        console.log('[' + BRIDGE_NAME + '] Initialized (v' + SCHEMA_VERSION + ', ' + platform + ') in ' + ms + 'ms');
      }, 0);
    })();
  } catch (e) {
    console.error('[Bridge] Init failed:', e);
  }
})();
"""
}
