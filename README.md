# check-mate

A unified, promise-based JavaScript bridge for bidirectional communication between web content and native mobile apps. `window.jsbridge` works identically on Android and iOS -- because life's too short for platform `if` statements.

---

## For Web Developers

### Quick Start

```js
await jsbridge.ready();

const info = await jsbridge.call({ data: { action: 'deviceInfo' } });
console.log(info.platform, info.model);

jsbridge.on((msg) => {
  const { action, content } = msg.data;
  if (action === 'lifecycle' && content.event === 'focused') refreshData();
});
```

That's the whole API. One method to call native (`call`), one to listen (`on`). Everything else is just different `action` strings.

### Core API

| Method | Description |
|--------|-------------|
| `jsbridge.ready()` | Returns a Promise that resolves when the bridge is initialized. Call this first. |
| `jsbridge.call(msg, opts?)` | Sends `{ data: { action, content? } }` to native. Returns a Promise with the response. |
| `jsbridge.on(fn)` | Registers a handler for native-to-web messages. Supports multiple handlers. |
| `jsbridge.off(fn)` | Removes a previously registered handler. |
| `jsbridge.setDebug(bool)` | Enables verbose console logging. |
| `jsbridge.setMockHandler(fn)` | Registers a mock for desktop browser testing. |
| `jsbridge.platform` | `'android'` \| `'ios'` \| `'desktop'` (read-only) |
| `jsbridge.schemaVersion` | Integer version set by native (read-only) |
| `jsbridge.getStats()` | Returns `{ pendingRequests, schemaVersion, platform, handlers, debugEnabled }` |

### Message Shape

Every call uses the same shape. Learn it once, use it forever:

```js
await jsbridge.call({
  data: {
    action: 'actionName',       // what to do
    content: { key: 'value' }   // optional payload
  }
}, { timeout: 5000 });          // optional timeout (default 30s)
```

### Actions Reference

#### Device & System

```js
// Device info
const info = await jsbridge.call({ data: { action: 'deviceInfo' } });
// → { platform, osVersion, model, appVersion, ... }

// Network status
const net = await jsbridge.call({ data: { action: 'networkState' } });
// → { connected: true, type: 'wifi' }

// System settings
await jsbridge.call({ data: { action: 'openSettings' } });

// Safe area insets (prefer CSS custom properties -- see Safe Area section)
const insets = await jsbridge.call({ data: { action: 'getInsets' } });
// → { statusBar: { height, visible }, systemNavigation: {...}, keyboard: {...}, safeArea: { top, right, bottom, left } }
```

#### UI

```js
await jsbridge.call({ data: { action: 'showToast', content: { message: 'Hey!', duration: 'short' } } });
await jsbridge.call({ data: { action: 'showAlert', content: { title: 'Hi', message: 'Hello', buttons: ['OK', 'Cancel'] } } });
await jsbridge.call({ data: { action: 'haptic', content: { vibrate: true } } });
await jsbridge.call({ data: { action: 'copyToClipboard', content: { text: 'copied!' } } });
```

#### Navigation

```js
// Top navigation bar
await jsbridge.call({ data: { action: 'topNavigation', content: { isVisible: true, title: 'Home', showUpArrow: false } } });

// Bottom navigation bar
await jsbridge.call({ data: { action: 'bottomNavigation', content: { isVisible: false } } });

// System bars (Android only -- iOS ignores this gracefully)
await jsbridge.call({ data: { action: 'systemBars', content: { showStatusBar: false, showSystemNavigation: false } } });

// URL navigation
await jsbridge.call({ data: { action: 'navigation', content: { url: 'https://example.com', external: true } } });
await jsbridge.call({ data: { action: 'navigation', content: { goBack: true } } });
```

#### Secure Storage

```js
await jsbridge.call({ data: { action: 'saveSecureData', content: { key: 'token', value: 'abc123' } } });
const { value } = await jsbridge.call({ data: { action: 'loadSecureData', content: { key: 'token' } } });
await jsbridge.call({ data: { action: 'removeSecureData', content: { key: 'token' } } });
```

Backed by Keychain (iOS) and EncryptedSharedPreferences (Android).

#### Lifecycle & Events

```js
// Enable lifecycle events from native
await jsbridge.call({ data: { action: 'lifecycleEvents', content: { enable: true } } });

// Listen for native-pushed events
jsbridge.on((msg) => {
  const { action, content } = msg.data;
  if (action === 'lifecycle') console.log(content.event); // 'focused' | 'defocused'
  if (action === 'onPushNotification') console.log(content);
});
```

#### Analytics (Fire-and-Forget)

Don't `await` these -- no response needed, zero latency:

```js
jsbridge.call({ data: { action: 'trackEvent', content: { event: 'button_click', params: { screen: 'home' } } } });
jsbridge.call({ data: { action: 'trackScreen', content: { screenName: 'Home' } } });
```

### Safe Area / Insets

Native automatically pushes CSS custom properties whenever bars change, the device rotates, or the keyboard appears. You don't need to call anything -- just use CSS:

```css
body {
  padding-top: var(--bridge-inset-top, env(safe-area-inset-top, 0px));
  padding-bottom: var(--bridge-inset-bottom, env(safe-area-inset-bottom, 0px));
}
```

Available properties: `--bridge-inset-top`, `--bridge-inset-right`, `--bridge-inset-bottom`, `--bridge-inset-left`, `--bridge-status-bar`, `--bridge-top-nav`, `--bridge-bottom-nav`, `--bridge-system-nav`.

Cascading fallback: bridge value → `env()` → `0px`. Works everywhere.

For JavaScript layout calculations, query on demand with `getInsets` (see Device & System above).

### Desktop Testing

When no native bridge is detected, `jsbridge.platform` returns `'desktop'`. Register a mock to test without an app:

```js
jsbridge.setMockHandler((msg) => {
  if (msg.data.action === 'deviceInfo') return { platform: 'desktop', model: 'Chrome' };
  return {};
});
```

The `index.html` demo page does this automatically.

### TypeScript

```typescript
interface Bridge {
  schemaVersion: number;
  platform: 'android' | 'ios' | 'desktop';
  ready(): Promise<void>;
  call<T = unknown>(msg: { data: { action: string; content?: unknown } }, opts?: { timeout?: number }): Promise<T>;
  on(handler: (msg: { data: { action: string; content?: unknown } }) => void): void;
  off(handler: Function): void;
  setDebug(enabled: boolean): void;
  setMockHandler(handler: Function | null): void;
  getStats(): { pendingRequests: number; schemaVersion: number; platform: string; handlers: number; debugEnabled: boolean };
}

declare global { interface Window { jsbridge: Bridge } }
```

### Tips

- **Always** `await jsbridge.ready()` before anything else
- **Set timeouts** on calls: `{ timeout: 5000 }` -- 30s default is generous
- **Don't await analytics** -- fire-and-forget is faster
- **Version-gate new features**: `if (jsbridge.schemaVersion >= 2) { ... }`
- **Cache device info** -- it doesn't change mid-session
- **Batch with `Promise.all()`** for parallel operations
- **Register `on()` once** during init, route by `action`

### Platform Support

| Action | iOS | Android |
|--------|:---:|:-------:|
| `deviceInfo` | ✅ | ✅ |
| `networkState` | ✅ | ✅ |
| `openSettings` | ✅ | ✅ |
| `getInsets` | ✅ | ✅ |
| `showToast` | ✅ | ✅ |
| `showAlert` | ✅ | ✅ |
| `topNavigation` | ✅ | ✅ |
| `bottomNavigation` | ✅ | ✅ |
| `systemBars` | -- | ✅ |
| `haptic` | ✅ | ✅ |
| `navigation` | ✅ | ✅ |
| `copyToClipboard` | ✅ | ✅ |
| `lifecycleEvents` | ✅ | ✅ |
| `saveSecureData` | ✅ | ✅ |
| `loadSecureData` | ✅ | ✅ |
| `removeSecureData` | ✅ | ✅ |
| `trackEvent` | ✅ | ✅ |
| `trackScreen` | ✅ | ✅ |

### Error Handling

```js
try {
  const info = await jsbridge.call({ data: { action: 'deviceInfo' } }, { timeout: 5000 });
} catch (e) {
  if (e.message.includes('timeout')) console.warn('Native not responding');
  else console.error(e.code, e.message);
}
```

Errors have `.code` (e.g. `UNKNOWN_ACTION`, `INVALID_PARAMETER`) and `.message`.

### Schema Versioning

Simple integer, auto-attached to every message. Native silently ignores messages from newer schema versions -- your call will timeout, and you can fallback:

```js
if (jsbridge.schemaVersion >= 2) {
  await jsbridge.call({ data: { action: 'fancyNewThing' } });
} else {
  oldApproach();
}
```

**Bump when:** breaking format changes, removing commands, incompatible behavior.  
**Don't bump when:** adding commands, adding optional fields, fixing bugs.

---

## For Native App Developers

### How It Works

The bridge is a thin decorator around each platform's native WebView messaging:

- **Android**: `@JavascriptInterface` on a `postMessage(String)` method + `evaluateJavascript()` for responses
- **iOS**: `WKScriptMessageHandler` + `evaluateJavaScript()` for responses

Both platforms inject the same JavaScript (`bridge.js`) into the WebView at document start. The JS auto-detects the platform and routes messages accordingly. Web developers see one API; you see your native APIs.

### Architecture

```
Web: window.jsbridge.call() / .on()
         │
    ┌────▼─────────────────────────────┐
    │  bridge.js (injected, identical) │  ← platform auto-detection
    │  Promise mgmt, timeouts, IDs     │
    └────┬────────────────────┬────────┘
         │                    │
    ┌────▼────┐         ┌────▼────┐
    │ Android │         │   iOS   │
    │ Bridge  │         │ Bridge  │
    └────┬────┘         └────┬────┘
         │                    │
    ┌────▼────────────────────▼────────┐
    │      Command Handler Registry    │  ← strategy pattern
    │  DeviceInfo, ShowToast, Haptic,  │
    │  Navigation, Storage, etc.       │
    └──────────────────────────────────┘
```

### Android Setup

```kotlin
// In your Composable or Fragment
val bridge = JavaScriptBridge(webView, DefaultBridgeMessageHandler())
webView.addJavascriptInterface(bridge, JavaScriptBridge.DEFAULT_BRIDGE_NAME)

// After page loads
bridge.injectBridgeScript()

// Send events to web
bridge.sendToWeb(action = "lifecycle", content = mapOf("event" to "focused"))

// Push safe area CSS whenever bars change
bridge.updateSafeAreaCSS(insetTop = 54, insetBottom = 34, statusBarHeight = 24, ...)
```

### iOS Setup

```swift
// In your ViewController
let bridge = JavaScriptBridge(webView: webView, viewController: self)
// Script injection happens automatically at document start

// Send events to web
bridge.sendToWeb(action: "lifecycle", content: ["event": "focused"])

// Push safe area CSS
bridge.updateSafeAreaCSS(insetTop: 54, insetBottom: 34, statusBarHeight: 24, ...)
```

### Adding a New Command

This is the whole point of the architecture. Three steps, ~50 lines total:

**1. Create the handler:**

```kotlin
// Android
class MyCommand : BridgeCommand {
    override val action = "myAction"
    override suspend fun handle(content: Any?): Any? {
        val param = BridgeParsingUtils.parseString(content, "param")
        return JSONObject().apply { put("result", "done: $param") }
    }
}
```

```swift
// iOS
class MyHandler: BridgeCommand {
    let actionName = "myAction"
    func handle(content: [String: Any]?, completion: @escaping (Result<[String: Any]?, BridgeError>) -> Void) {
        let param = content?["param"] as? String ?? ""
        completion(.success(["result": "done: \(param)"]))
    }
}
```

**2. Register it (one line):**

```kotlin
// Android: DefaultBridgeMessageHandler.kt
private val commands = listOf(
    // ...existing commands...
    MyCommand()  // ← done
)
```

```swift
// iOS: JavaScriptBridge.swift registerCommandHandlers()
register(handler: MyHandler())  // ← done
```

**3. Call from web:**

```js
const result = await jsbridge.call({ data: { action: 'myAction', content: { param: 'hello' } } });
```

You didn't touch the bridge infrastructure, the JS layer, or any other command. That's the beauty of the command pattern.

### Changing the Bridge Name

```kotlin
// Android
val bridge = JavaScriptBridge(webView, handler, bridgeName = "myApp")
webView.addJavascriptInterface(bridge, "myApp")
```

```swift
// iOS
let bridge = JavaScriptBridge(webView: webView, viewController: self, bridgeName: "myApp")
```

One parameter. Everything else -- the JS global, the callbacks, the message handler name -- updates automatically.

### Response Format

Standardized across both platforms: `{ id, data?, error? }`. If `error` is present, it's a failure. If `data` is present (or both absent), it's a success. Return `null` from a command handler for fire-and-forget (no response sent).

### Message Protocol

```json
{
  "id": "msg_1234567890_1_abc123xyz",
  "version": 1,
  "data": {
    "action": "showToast",
    "content": { "message": "Hello!" }
  }
}
```

Version checking: if `version > SCHEMA_VERSION`, silently ignore. Web will timeout and fallback.

### Security

- The `window.jsbridge` object is `Object.freeze()`'d and non-writable
- Android: only `@JavascriptInterface`-annotated methods are exposed
- iOS: WKScriptMessageHandler with named handler registration
- All commands validate their input
- Threading: Android `@JavascriptInterface` runs on a background thread -- all WebView operations are dispatched to main

### Native-Driven Safe Area CSS

Instead of making web poll for insets, native proactively pushes CSS custom properties:

```kotlin
// Android: call whenever bars change, on rotation, keyboard show/hide
bridge.updateSafeAreaCSS(
    insetTop = statusBarHeight + topNavHeight,
    insetBottom = bottomNavHeight + systemNavHeight,
    statusBarHeight = statusBarHeight,
    topNavHeight = topNavHeight,
    bottomNavHeight = bottomNavHeight,
    systemNavHeight = systemNavHeight
)
```

This is strictly better than injecting `padding-top: Xpx !important` because web controls where and how to use the values.

---

## Project Structure

```
check-mate/
├── bridge.js                  # Unified JS (single source of truth, injected by both platforms)
├── index.html                 # Demo page (symlinked into both sample apps)
├── android-sample/            # Android sample (Kotlin, Compose, Gradle)
│   └── .../bridge/
│       ├── JavaScriptBridge.kt
│       ├── DefaultBridgeMessageHandler.kt
│       └── commands/          # One file per action
├── ios-sample/                # iOS sample (Swift, SwiftUI, Xcode)
│   └── BridgeSample/Bridge/
│       ├── JavaScriptBridge.swift
│       ├── BridgeCommand.swift
│       └── Commands/          # One file per action
└── CHANGELOG.md
```

### Building

**Android:** Open `android-sample/` in Android Studio, hit Run.  
**iOS:** Open `ios-sample/BridgeSample.xcodeproj` in Xcode, Cmd+R.

Both apps load the same `index.html` demo page with all bridge features.

---

## Performance

| Operation | Latency | Pattern |
|-----------|---------|---------|
| Fire-and-forget (analytics, haptic) | < 1ms | No `await` |
| Simple queries (deviceInfo) | 5-15ms | `await` |
| UI operations (toast, alert) | 10-30ms | `await` |
| Secure storage | 20-50ms | `await` |

Batch with `Promise.all()`. Cache what doesn't change. Don't await what doesn't matter.

---

## Changelog

### 2.0.0 (2026-03-04)

Unified bridge release. Breaking changes:

- Bridge global renamed from `window.bridge` to `window.jsbridge` (configurable)
- Response format standardized to `{ id, data?, error? }` (iOS `success` field removed)
- `on()` now supports multiple handlers (use `off()` to remove)
- Unified callback names across platforms

New: `bridge.js` single source of truth, `platform` property, `off()`, `setMockHandler()`, `getStats()`, `getInsets` action, native-driven safe area CSS injection, desktop mock mode.

### 1.0.0

Initial release. `call()`, `on()`, `ready()`, `setDebug()`. Android and iOS samples. Schema versioning. Command pattern.

---

## License

See [LICENSE](LICENSE) file.
