# JSBridge for Android

[![Android CI](https://github.com/kibotu/jsbridge/actions/workflows/android.yml/badge.svg)](https://github.com/kibotu/jsbridge/actions/workflows/android.yml)
[![Maven Central](https://img.shields.io/maven-central/v/net.kibotu/jsbridge)](https://central.sonatype.com/artifact/net.kibotu/jsbridge)
[![JitPack](https://jitpack.io/v/kibotu/jsbridge.svg)](https://jitpack.io/#kibotu/jsbridge)

A Kotlin library that gives your WebView a promise-based JavaScript bridge. Web content calls native with `await jsbridge.call(...)`, native pushes events back with `bridge.sendToWeb(...)`. One API, no ceremony.

## Requirements

- Android API 24+ (minSdk 24)
- JDK 21 (to build from source)
- Kotlin

## Installation

### Maven Central

```kotlin
dependencies {
    implementation("net.kibotu:jsbridge:<version>")
}
```

### JitPack

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.kibotu:jsbridge:<version>")
}
```

### Local Module

If you've cloned the repo:

```kotlin
// settings.gradle.kts
include(":jsbridge")

// app/build.gradle.kts
dependencies {
    implementation(project(":jsbridge"))
}
```

## Quick Start

```kotlin
val bridge = JavaScriptBridge.inject(
    webView = webView,
    commands = DefaultCommands.all()
)
```

That's the whole setup. Your web content can now call native:

```js
const info = await jsbridge.call('deviceInfo');
```

## Usage

### Pick Your Commands

Use all defaults, or only what you need:

```kotlin
// Everything
val bridge = JavaScriptBridge.inject(
    webView = webView,
    commands = DefaultCommands.all()
)

// Just the essentials
val bridge = JavaScriptBridge.inject(
    webView = webView,
    commands = listOf(
        DeviceInfoCommand(),
        ShowToastCommand(),
        HapticCommand(),
    )
)
```

Commands that need the bridge (for safe-area updates, context access, etc.) implement `BridgeAware` and are wired automatically by `inject()`.

### Send Events to Web

```kotlin
bridge.sendToWeb(action = "lifecycle", content = mapOf("event" to "focused"))
```

### Custom Bridge Name

The default global is `window.jsbridge`. Change it if you need to:

```kotlin
val bridge = JavaScriptBridge.inject(
    webView = webView,
    commands = DefaultCommands.all(),
    bridgeName = "myApp"
)
```

Web code then uses `await myApp.call(...)` instead.

### Multiple Bridges

Register separate bridges on the same WebView for different concerns:

```kotlin
val mainBridge = JavaScriptBridge.inject(
    webView = webView, bridgeName = "jsbridge",
    commands = listOf(DeviceInfoCommand(), ShowToastCommand())
)
val analyticsBridge = JavaScriptBridge.inject(
    webView = webView, bridgeName = "analytics",
    commands = listOf(TrackEventCommand(), TrackScreenCommand())
)
```

### Retrieve an Existing Bridge

```kotlin
val bridge = webView.bridge()                    // default name
val analytics = webView.bridge("analytics")      // by name
```

## Adding a Custom Command

Implement `BridgeCommand`. That's the whole contract:

```kotlin
class MyCommand : BridgeCommand {
    override val action = "myAction"

    override suspend fun handle(content: Any?): Any? {
        val params = content as? JSONObject
        val name = params?.optString("name") ?: "world"
        return JSONObject().apply {
            put("greeting", "hello, $name")
        }
    }
}
```

Register it:

```kotlin
val bridge = JavaScriptBridge.inject(
    webView = webView,
    commands = DefaultCommands.all() + MyCommand()
)
```

Call it from web:

```js
const result = await jsbridge.call('myAction', { name: 'Kotlin' });
// → { greeting: "hello, Kotlin" }
```

Return `null` for fire-and-forget commands that don't need a response.

## Lifecycle & Focus

Web content has no way to know it's been covered by a dialog, buried in a ViewPager, or backgrounded. Push lifecycle events so it can react:

```kotlin
override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    bridge.sendToWeb("lifecycle", mapOf("event" to if (hasFocus) "focused" else "defocused"))
    if (hasFocus) SafeAreaService.pushToBridge(bridge)
}
```

### Cleanup

Call `destroy()` when the hosting component is torn down:

```kotlin
DisposableEffect(Unit) {
    onDispose { bridge.destroy() }
}
```

## Safe Area

Native automatically pushes CSS custom properties whenever bars change, the device rotates, or the screen regains focus. Web content doesn't poll -- it just uses CSS:

```css
body {
  padding-top: var(--bridge-inset-top, env(safe-area-inset-top, 0px));
  padding-bottom: var(--bridge-inset-bottom, env(safe-area-inset-bottom, 0px));
}
```

Programmatic push:

```kotlin
bridge.updateSafeAreaCSS(
    insetTop = statusBarHeight + topNavHeight,
    insetBottom = bottomNavHeight + systemNavHeight,
    statusBarHeight = statusBarHeight,
    topNavHeight = topNavHeight,
    bottomNavHeight = bottomNavHeight,
    systemNavHeight = systemNavHeight
)
```

`SafeAreaService` handles this automatically when using `DefaultCommands`. The `BridgeWebViewClient` pushes on `onPageFinished`.

## Jetpack Compose

The sample app shows the full Compose integration:

```kotlin
@Composable
fun WebViewScreen(url: String, onBridgeReady: (JavaScriptBridge) -> Unit) {
    val bridgeState = remember { mutableStateOf<JavaScriptBridge?>(null) }

    DisposableEffect(Unit) {
        onDispose { bridgeState.value?.destroy() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                val bridge = JavaScriptBridge.inject(
                    webView = this,
                    commands = DefaultCommands.all()
                )
                bridgeState.value = bridge
                onBridgeReady(bridge)
                loadUrl(url)
            }
        }
    )
}
```

## Secure Storage

The `saveSecureData`, `loadSecureData`, and `removeSecureData` commands use EncryptedSharedPreferences backed by Android Tink. These dependencies are `compileOnly` -- add them to your app if you use secure storage:

```kotlin
dependencies {
    implementation("com.google.crypto.tink:tink-android:<version>")
    implementation("androidx.security:security-crypto:<version>")
}
```

## Built-in Commands

| Action | Description |
|--------|-------------|
| `deviceInfo` | Platform, OS version, model, app version |
| `networkState` | Connectivity status and type |
| `openSettings` | Opens the app's Settings page |
| `insets` | Current safe area insets and bar heights (dp values) |
| `systemBarsInfo` | Individual bar dimensions and visibility (dp values) |
| `showToast` | Native toast message |
| `showAlert` | Native alert dialog |
| `topNavigation` | Show/hide/configure the top navigation bar |
| `bottomNavigation` | Show/hide the bottom navigation bar |
| `systemBars` | Show/hide status bar and system navigation |
| `haptic` | Haptic feedback and vibration |
| `navigation` | Load URLs or go back |
| `copyToClipboard` | Copy text to clipboard |
| `requestPermissions` | Request runtime permissions |
| `saveSecureData` | Store data in EncryptedSharedPreferences |
| `loadSecureData` | Read data from EncryptedSharedPreferences |
| `removeSecureData` | Delete data from EncryptedSharedPreferences |
| `trackEvent` | Analytics event (fire-and-forget) |
| `trackScreen` | Analytics screen view (fire-and-forget) |
| `themeChanged` | Respond to theme/appearance changes |
| `refresh` | Trigger a page refresh |

## Building the Sample App

```bash
cd android
./gradlew :sample:installDebug
```

Or open the `android/` folder in Android Studio and run the `:sample` module. The sample app loads `index.html` with all bridge features wired up.

## Debugging

Connect your device via USB, enable USB debugging, then open `chrome://inspect` in Chrome:

```js
await jsbridge.call('deviceInfo')
jsbridge.setDebug(true)    // logs every message
jsbridge.getStats()        // bridge state snapshot
```

## Project Structure

```
android/
├── jsbridge/                              # Library module
│   ├── build.gradle.kts
│   └── src/main/java/net/kibotu/jsbridge/
│       ├── JavaScriptBridge.kt            # Core bridge
│       ├── DefaultCommands.kt             # All built-in commands
│       ├── SafeAreaService.kt             # CSS custom property injection
│       ├── SecureStorage.kt               # EncryptedSharedPreferences wrapper
│       ├── commands/                      # One file per action
│       │   ├── BridgeCommand.kt           # Command interface
│       │   ├── BridgeError.kt             # Error types
│       │   └── ...
│       └── decorators/                    # WebViewClient/ChromeClient wrappers
│           ├── BridgeWebViewClient.kt
│           └── ...
├── sample/                                # Sample app
│   └── src/main/java/net/kibotu/bridgesample/
└── gradle.properties                      # Maven coordinates & metadata
```

## Publishing

Publishing to Maven Central happens automatically via CI when a semver tag is pushed (e.g. `3.1.0`). JitPack builds are triggered automatically by any tag or commit.

## License

See [LICENSE](../LICENSE).
