# AGENTS.md

## Layout

- `bridge.js` — the JS bridge (no build, no deps). Root copy is the source of truth; byte-identical copies live at `android/jsbridge/src/main/res/raw/bridge.js` and `ios/JSBridge/Sources/JSBridge/Resources/bridge.js`. `test/sync.test.js` fails if they drift — **edit all three together**.
- `bridge.js` is a template with exactly two placeholders: `__BRIDGE_NAME__` (window API name) and `__FEATURES__` (JSON array of feature names). Android fills it in `BridgeScript.kt`, iOS in `BridgeScript.swift`, from the registered commands' `features`.
- `android/` — Gradle root project "BridgeSample", modules `:jsbridge` (library) and `:sample` (app `net.kibotu.bridgesample`).
- `ios/` — `JSBridge` (Swift package, root `Package.swift` points the target at `ios/JSBridge/Sources/JSBridge`) and `BridgeSample`.
- `index.html` — self-contained demo page loaded by both sample apps; deployed to GitHub Pages on push to `main`. highlight.js is inlined by `npm run update:highlightjs`, not from a CDN.
- `progress.md` is gitignored local working notes, not repo docs. Don't commit or quote it.

## Commands

```bash
npm test                                        # JS tests, node --test, no deps (Node 22)
cd android && ./gradlew :jsbridge:testDebugUnitTest
cd ios/JSBridge && swift test
maestro test .maestro/                          # Android sample UI flows (needs a device/emulator)
```

- Tests cover pure logic only (`test/harness.js` evals bridge.js in a `vm`); no Robolectric, no UI unit tests. UI features are verified via the demo page checklist and Maestro.
- Android Gradle toolchain is JDK 21, auto-provisioned via the foojay resolver; a system JDK 17 is fine (that's what CI uses).
- No lint/typecheck config exists. `npm test` and unit tests are the whole gate locally.

## CI / releases

- Workflows filter by path: `android.yml` runs on `android/**` only, `web.yml` on `bridge.js` + `test/**`. A `bridge.js` change alone does **not** run Android/iOS CI — run those tests yourself.
- Android publishes to Maven Central on a semver git tag (`publishAndReleaseToMavenCentral`, credentials from secrets). JitPack also works but publishes under `com.github.kibotu` (see `jitpack.yml`, JDK 17 there).
- iOS/SPM releases are just git tags; tag only after iOS builds (`xcodebuild` in `ios.yml`). Past releases have shipped broken iOS builds — 2.0.0 needed a 2.0.1 hotfix.

## Conventions that change how you write code

- One class per action in `commands/` (Kotlin) and `Commands/` (Swift); keep both platforms in parity. Register via `DefaultCommands.all() + CustomCommand()`. An unregistered action rejects with `UNKNOWN_ACTION`.
- Command features are flat strings (`haptic`, `haptic.pattern`). **A changed capability gets a new name; names never contain numbers.** `jsbridge.features` is the visible contract.
- Contributions to `supports()` are derived from registered commands automatically — add finer names by overriding `features`, not by touching `bridge.js`.
- Errors returned to the web side need a `code` stable enough for pages to switch on.
- Releasing requires a `CHANGELOG.md` entry (Keep a Changelog / semver).
