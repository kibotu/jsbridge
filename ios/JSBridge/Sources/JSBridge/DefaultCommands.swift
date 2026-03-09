import Foundation
import UIKit
import WebKit

/// Convenience factory providing all built-in bridge commands.
///
/// Commands are **not** registered automatically. This helper lets
/// an app opt-in to all defaults with a single call.
@MainActor
public struct DefaultCommands {

    /// Returns all built-in commands.
    ///
    /// - Parameters:
    ///   - viewController: The hosting view controller (needed by UI commands like alerts/toasts).
    ///   - webView: The WKWebView (needed by navigation commands).
    ///   - bridge: Optional bridge reference for commands that push safe-area updates.
    public static func all(
        viewController: UIViewController? = nil,
        webView: WKWebView? = nil,
        bridge: JavaScriptBridge? = nil
    ) -> [any BridgeCommand] {
        return [
            DeviceInfoCommand(),
            NetworkStatusCommand(),
            OpenSettingsCommand(),

            ShowToastCommand(viewController: viewController),
            ShowAlertCommand(viewController: viewController),

            TopNavigationCommand(bridge: bridge),
            BottomNavigationCommand(bridge: bridge),
            NavigationCommand(viewController: viewController, webView: webView),
            OpenUrlCommand(),

            SystemBarsCommand(),
            GetInsetsCommand(viewController: viewController),
            HapticCommand(),
            CopyToClipboardCommand(),
            RequestPermissionsCommand(),

            SaveSecureDataCommand(),
            LoadSecureDataCommand(),
            RemoveSecureDataCommand(),

            TrackEventCommand(),
            TrackScreenCommand(),

            ThemeChangedCommand(),

            RefreshCommand(),
        ]
    }
}
