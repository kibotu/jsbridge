import Foundation

/// Convenience factory providing all built-in bridge commands.
///
/// Commands are **not** registered automatically. This helper lets
/// an app opt-in to all defaults with a single call:
///
/// ```swift
/// let bridge = JavaScriptBridge(
///     webView: webView,
///     viewController: self,
///     commands: DefaultCommands.all()
/// )
/// ```
///
/// Commands that need the bridge (or its `viewController` / `webView`) implement
/// ``BridgeAware`` and are wired automatically by ``JavaScriptBridge``.
@MainActor
public struct DefaultCommands {
    /// Returns all built-in commands.
    public static func all() -> [any BridgeCommand] {
        [
            DeviceInfoCommand(),
            NetworkStatusCommand(),
            OpenSettingsCommand(),

            ShowToastCommand(),
            ShowAlertCommand(),

            TopNavigationCommand(),
            BottomNavigationCommand(),
            NavigationCommand(),

            SystemBarsCommand(),
            SystemBarsInfoCommand(),
            GetInsetsCommand(),
            HapticCommand(),
            CopyToClipboardCommand(),
            RequestPermissionsCommand(),

            SaveSecureDataCommand(),
            LoadSecureDataCommand(),
            RemoveSecureDataCommand(),

            TrackEventCommand(),
            TrackScreenCommand(),

            ThemeChangedCommand(),

            RefreshCommand()
        ]
    }
}
