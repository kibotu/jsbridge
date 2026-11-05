# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.0] - 2026-11-05

### Added

- `jsbridge.supports(feature)` and `jsbridge.features` for feature detection. Each command contributes its action name automatically. Override `features` on a `BridgeCommand` to declare finer names, for example `haptic.pattern`.
- `setMockHandler(fn, { features })` declares which features a desktop mock implements.
- Automated tests: `npm test` (JavaScript), `:jsbridge:testDebugUnitTest` (Android) and `swift test` in `ios/JSBridge` (iOS).
- [Maestro](https://maestro.mobile.dev/) UI flows for the Android sample in `.maestro/`.
- The iOS sample app makes its WebView inspectable in debug builds (iOS 16.4+).

### Changed

- **BREAKING:** The `bridgeReady` event detail is now `{ platform, features }`.
- Android `minSdk` is now 24 (was 23).
- Android build uses `compileSdk` 37, `targetSdk` 37, Android Gradle Plugin 9.5.1, Kotlin 2.4.21 and JDK 21.
- The demo page inlines highlight.js instead of loading CodeMirror from a CDN. It now works offline and behind proxies. `npm run update:highlightjs` refreshes the inlined copy.
- GitHub Actions workflows use newer action versions.

### Removed

- **BREAKING:** Schema versioning. This removes `jsbridge.schemaVersion`, the `version` message field, the `__SCHEMA_VERSION__` template variable, `CallOptions.version`, `JavaScriptBridge.SCHEMA_VERSION` (Android) and `BridgeError.UnsupportedVersion` (iOS). Native code ignores a legacy `version` field. Use `jsbridge.supports(feature)` instead.

## [1.0.9] - 2026-03-18

### Added

- iOS debug builds forward the WebView `console.*` output to the native log.

## [1.0.8] - 2026-03-10

### Changed

- The `JSBridge` Swift package in `ios/JSBridge` depends on Orchard 1.0.9 (was 1.0.8).

## [1.0.7] - 2026-03-10

### Added

- `systemBarsInfo` action on Android and iOS. It returns the height and visibility of each system bar.

### Changed

- The `insets` action returns more detail about each bar.

## [1.0.6] - 2026-03-10

### Added

- iOS commands that implement `BridgeAware` receive the bridge automatically.
- `JavaScriptBridge.webView` and `JavaScriptBridge.viewController` are readable on iOS.

### Changed

- Android parsing helpers accept missing `title` and parameter values without crashing, so `title` in `topNavigation` is optional.
- Demo page and README animations show the current behavior.

## [1.0.5] - 2026-03-09

### Added

- Android README with installation and usage notes.

### Fixed

- JitPack build configuration.

## [1.0.4] - 2026-03-09

### Fixed

- Android library publishing configuration in CI.

## [1.0.3] - 2026-03-09

### Added

- `jitpack.yml` to publish the Android library through [JitPack](https://jitpack.io/#kibotu/jsbridge).

## [1.0.2] - 2026-03-09

### Added

- Publishing to [Maven Central](https://central.sonatype.com/artifact/net.kibotu/jsbridge) as `net.kibotu:jsbridge`.
- iOS README.

### Fixed

- CI builds.
- Android `haptic` command.

## [1.0.1] - 2026-03-09

### Added

- `Package.swift` in the repository root. Swift Package Manager can now resolve `https://github.com/kibotu/jsbridge` directly.

## [1.0.0] - 2026-03-09

First tagged release.

### Added

- `bridge.js`: one JavaScript file, shared by Android and iOS, that exposes `window.jsbridge` with `ready()`, `call()`, `on()`, `off()`, `cancelAll()`, `setDebug()`, `setMockHandler()`, `getStats()` and `platform`.
- Android library module `net.kibotu.jsbridge` and iOS Swift package `JSBridge`.
- Opt-in commands: pass `DefaultCommands.all()` or pick single commands.
- Built-in actions: `deviceInfo`, `networkState`, `openSettings`, `requestPermissions`, `insets`, `showToast`, `showAlert`, `haptic`, `copyToClipboard`, `topNavigation`, `bottomNavigation`, `systemBars`, `navigation`, `saveSecureData`, `loadSecureData`, `removeSecureData`, `trackEvent`, `trackScreen`, `refresh` and `themeChanged`.
- Native-driven safe area CSS custom properties (`--bridge-inset-*`).
- `lifecycle` events (`focused` and `defocused`).
- Several named bridges on one WebView.
- Desktop mock mode.
- Android and iOS sample apps with a shared `index.html` demo page.
- Continuous integration for Android and iOS.

[Unreleased]: https://github.com/kibotu/jsbridge/compare/2.0.0...HEAD
[2.0.0]: https://github.com/kibotu/jsbridge/compare/1.0.9...2.0.0
[1.0.9]: https://github.com/kibotu/jsbridge/compare/1.0.8...1.0.9
[1.0.8]: https://github.com/kibotu/jsbridge/compare/1.0.7...1.0.8
[1.0.7]: https://github.com/kibotu/jsbridge/compare/1.0.6...1.0.7
[1.0.6]: https://github.com/kibotu/jsbridge/compare/1.0.5...1.0.6
[1.0.5]: https://github.com/kibotu/jsbridge/compare/1.0.4...1.0.5
[1.0.4]: https://github.com/kibotu/jsbridge/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/kibotu/jsbridge/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/kibotu/jsbridge/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/kibotu/jsbridge/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/kibotu/jsbridge/releases/tag/1.0.0
