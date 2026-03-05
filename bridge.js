/**
 * Unified JavaScript Bridge -- platform-agnostic, promise-based communication
 * between web content and native mobile apps (Android & iOS).
 *
 * This file is the single source of truth. Both Android and iOS embed it
 * verbatim, templating in BRIDGE_NAME and SCHEMA_VERSION at injection time.
 *
 * Native template variables (replaced before injection):
 *   __BRIDGE_NAME__    -- e.g. "jsbridge"  (the window.* property name for public API)
 *   __SCHEMA_VERSION__ -- e.g. 1           (integer)
 *
 * The native interface is registered under the same name on both platforms:
 *   Android: addJavascriptInterface(bridge, BRIDGE_NAME)  --> window.jsbridge.postMessage()
 *   iOS:     add(self, name: BRIDGE_NAME)                 --> webkit.messageHandlers.jsbridge
 *
 * On Android, addJavascriptInterface runs before script injection, so the
 * native object is already on window[BRIDGE_NAME]. We stash a reference to it
 * before overwriting the property with the frozen public API.
 *
 * @see https://github.com/kibotu/check-mate
 */
(function () {
  'use strict';

  try {
    (function initializeBridge() {
      var BRIDGE_NAME = '__BRIDGE_NAME__';
      var SCHEMA_VERSION = __SCHEMA_VERSION__;
      var DEFAULT_TIMEOUT = 30000;
      var CALLBACK_RESPONSE = '__' + BRIDGE_NAME + '_handleResponse';
      var CALLBACK_MESSAGE = '__' + BRIDGE_NAME + '_handleNativeMessage';

      // On Android, the @JavascriptInterface object is already on window[BRIDGE_NAME]
      // before this script runs. Grab a reference before we overwrite it.
      var nativeAndroid = (window[BRIDGE_NAME] && typeof window[BRIDGE_NAME].postMessage === 'function')
        ? window[BRIDGE_NAME]
        : null;

      // On iOS, the WKScriptMessageHandler lives under webkit.messageHandlers
      var nativeIOS = (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers[BRIDGE_NAME])
        ? window.webkit.messageHandlers[BRIDGE_NAME]
        : null;

      // Guard against double init (e.g. bridge script injected twice)
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
      var readyPromise = new Promise(function (resolve) {
        readyResolve = resolve;
      });

      // -- helpers --------------------------------------------------------

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

        if (nativeAndroid) {
          nativeAndroid.postMessage(str);
          return;
        }

        if (nativeIOS) {
          nativeIOS.postMessage(str);
          return;
        }

        // Desktop mock mode
        if (mockHandler) {
          try {
            var result = mockHandler(message);
            if (result && typeof result.then === 'function') {
              result.then(function (data) {
                handleResponse({ id: message.id, data: data });
              }).catch(function (err) {
                handleResponse({ id: message.id, error: { code: 'MOCK_ERROR', message: err.message || String(err) } });
              });
            } else {
              setTimeout(function () {
                handleResponse({ id: message.id, data: result });
              }, 0);
            }
          } catch (err) {
            setTimeout(function () {
              handleResponse({ id: message.id, error: { code: 'MOCK_ERROR', message: err.message || String(err) } });
            }, 0);
          }
          return;
        }

        throw new Error('Native bridge not available. Running on desktop? Use ' + BRIDGE_NAME + '.setMockHandler(fn) for testing.');
      }

      function cleanupPromise(id) {
        var entry = pendingPromises[id];
        if (entry && entry.timeoutId) clearTimeout(entry.timeoutId);
        delete pendingPromises[id];
      }

      // -- response / message callbacks -----------------------------------

      function handleResponse(response) {
        debugLog('Received response:', response);

        if (!response || typeof response !== 'object' || !response.id) {
          console.error('[' + BRIDGE_NAME + '] Invalid response format:', response);
          return;
        }

        var entry = pendingPromises[response.id];
        if (!entry) {
          debugLog('No pending promise for id: ' + response.id);
          return;
        }

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

        if (messageHandlers.length === 0) {
          debugLog('No message handlers registered');
          return;
        }

        for (var i = 0; i < messageHandlers.length; i++) {
          try {
            messageHandlers[i](message);
          } catch (err) {
            console.error('[' + BRIDGE_NAME + '] Error in message handler:', err);
          }
        }
      }

      // -- public API -----------------------------------------------------

      var bridge = {
        schemaVersion: SCHEMA_VERSION,

        ready: function () {
          return readyPromise;
        },

        setDebug: function (enabled) {
          debug = Boolean(enabled);
          debugLog('Debug logging ' + (debug ? 'enabled' : 'disabled'));
        },

        call: function (actionOrMessage, contentOrOptions, options) {
          var message;
          var opts;
          if (typeof actionOrMessage === 'string') {
            message = { data: { action: actionOrMessage } };
            if (contentOrOptions != null) message.data.content = contentOrOptions;
            opts = options || {};
          } else {
            message = actionOrMessage;
            opts = contentOrOptions || {};
          }

          return new Promise(function (resolve, reject) {
            var id;
            try {
              validateMessage(message);
              id = generateId();
              var timeout = opts.timeout != null ? opts.timeout : DEFAULT_TIMEOUT;
              var version = opts.version != null ? opts.version : SCHEMA_VERSION;

              var fullMessage = {
                version: version,
                id: id,
                data: message.data
              };

              debugLog('Calling native:', fullMessage);

              var timeoutId = setTimeout(function () {
                cleanupPromise(id);
                reject(new Error('Request timeout after ' + timeout + 'ms'));
              }, timeout);

              pendingPromises[id] = {
                resolve: resolve,
                reject: reject,
                timeoutId: timeoutId
              };

              sendToNative(fullMessage);
            } catch (err) {
              if (id) cleanupPromise(id);
              debugLog('Call failed:', err);
              reject(err);
            }
          });
        },

        on: function (handler) {
          if (typeof handler !== 'function') throw new Error('Handler must be a function');
          messageHandlers.push(handler);
          debugLog('Message handler registered (' + messageHandlers.length + ' total)');
        },

        off: function (handler) {
          var idx = messageHandlers.indexOf(handler);
          if (idx !== -1) {
            messageHandlers.splice(idx, 1);
            debugLog('Message handler removed (' + messageHandlers.length + ' remaining)');
          }
        },

        setMockHandler: function (handler) {
          if (handler !== null && typeof handler !== 'function') throw new Error('Mock handler must be a function or null');
          mockHandler = handler;
          if (handler) {
            _platform = 'desktop';
            debugLog('Mock handler set -- desktop testing mode active');
          }
        },

        cancelAll: function () {
          var ids = Object.keys(pendingPromises);
          var err = new Error('All pending requests cancelled');
          err.code = 'CANCELLED';
          for (var i = 0; i < ids.length; i++) {
            var entry = pendingPromises[ids[i]];
            cleanupPromise(ids[i]);
            entry.reject(err);
          }
          debugLog('Cancelled ' + ids.length + ' pending request(s)');
        },

        getStats: function () {
          return {
            pendingRequests: Object.keys(pendingPromises).length,
            schemaVersion: SCHEMA_VERSION,
            platform: _platform,
            handlers: messageHandlers.length,
            debugEnabled: debug
          };
        }
      };

      Object.defineProperty(bridge, 'platform', {
        get: function () { return _platform; },
        enumerable: true
      });

      // Expose global callbacks for native to invoke
      window[CALLBACK_RESPONSE] = handleResponse;
      window[CALLBACK_MESSAGE] = handleNativeMessage;

      // Freeze and expose
      Object.defineProperty(window, BRIDGE_NAME, {
        value: Object.freeze(bridge),
        writable: false,
        configurable: false
      });

      // Ready on next tick so page scripts can register listeners first
      setTimeout(function () {
        var ms = (performance.now() - initStart).toFixed(2);
        readyResolve();
        try {
          window.dispatchEvent(new CustomEvent('bridgeReady', {
            detail: { schemaVersion: SCHEMA_VERSION, platform: _platform }
          }));
        } catch (e) {
          console.error('[' + BRIDGE_NAME + '] Failed to dispatch ready event:', e);
        }
        console.log('[' + BRIDGE_NAME + '] Initialized (v' + SCHEMA_VERSION + ', ' + _platform + ') in ' + ms + 'ms');
      }, 0);
    })();
  } catch (err) {
    console.error('[Bridge] Initialization failed:', err);
  }
})();
