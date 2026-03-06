package net.kibotu.jsbridge

import android.content.Context
import net.kibotu.jsbridge.commands.BridgeCommand
import net.kibotu.jsbridge.commands.NetworkStatusCommand
import net.kibotu.jsbridge.commands.CopyToClipboardCommand
import net.kibotu.jsbridge.commands.DeviceInfoCommand
import net.kibotu.jsbridge.commands.GetInsetsCommand
import net.kibotu.jsbridge.commands.HapticCommand
import net.kibotu.jsbridge.commands.LoadSecureDataCommand
import net.kibotu.jsbridge.commands.NavigationCommand
import net.kibotu.jsbridge.commands.OpenSettingsCommand
import net.kibotu.jsbridge.commands.OpenUrlCommand
import net.kibotu.jsbridge.commands.RemoveSecureDataCommand
import net.kibotu.jsbridge.commands.RequestPermissionsCommand
import net.kibotu.jsbridge.commands.SaveSecureDataCommand
import net.kibotu.jsbridge.commands.ShowAlertCommand
import net.kibotu.jsbridge.commands.ShowToastCommand
import net.kibotu.jsbridge.commands.bottomnavigation.BottomNavigationCommand
import net.kibotu.jsbridge.commands.refresh.RefreshCommand
import net.kibotu.jsbridge.commands.systembars.SystemBarsCommand
import net.kibotu.jsbridge.commands.topnavigation.TopNavigationCommand
import net.kibotu.jsbridge.commands.theme.ThemeChangedCommand
import net.kibotu.jsbridge.commands.tracking.TrackEventCommand
import net.kibotu.jsbridge.commands.tracking.TrackScreenCommand

/**
 * Convenience factory providing all built-in bridge commands.
 *
 * Commands are **not** registered automatically. This helper lets
 * an app opt-in to all defaults with a single call:
 *
 * ```kotlin
 * val bridge = JavaScriptBridge.inject(
 *     webView = webView,
 *     commands = DefaultCommands.all(getBridge = { bridge })
 * )
 * ```
 */
object DefaultCommands {

    /**
     * Returns all built-in commands.
     *
     * @param getBridge Lazy accessor for the bridge instance, required by
     *   commands that push safe-area updates back to the WebView
     *   (e.g. TopNavigationCommand, BottomNavigationCommand).
     *   Also used to derive the [Context] for commands that need it.
     */
    fun all(getBridge: () -> JavaScriptBridge? = { null }): List<BridgeCommand> {
        val contextProvider: () -> Context? = { getBridge()?.context }
        return listOf(
            DeviceInfoCommand(contextProvider),
            NetworkStatusCommand(contextProvider),
            SystemBarsCommand(contextProvider),
            GetInsetsCommand(contextProvider),
            HapticCommand(contextProvider),
            RequestPermissionsCommand(),
            OpenSettingsCommand(contextProvider),
            CopyToClipboardCommand(contextProvider),
            OpenUrlCommand(),
            NavigationCommand(contextProvider),
            TopNavigationCommand(getBridge),
            BottomNavigationCommand(getBridge),
            ShowToastCommand(contextProvider),
            ShowAlertCommand(contextProvider),
            SaveSecureDataCommand(contextProvider),
            LoadSecureDataCommand(contextProvider),
            RemoveSecureDataCommand(contextProvider),
            ThemeChangedCommand(),
            TrackEventCommand(),
            TrackScreenCommand(),
            RefreshCommand()
        )
    }
}
