package net.kibotu.jsbridge

import net.kibotu.jsbridge.commands.BridgeCommand
import net.kibotu.jsbridge.commands.CopyToClipboardCommand
import net.kibotu.jsbridge.commands.DeviceInfoCommand
import net.kibotu.jsbridge.commands.GetInsetsCommand
import net.kibotu.jsbridge.commands.HapticCommand
import net.kibotu.jsbridge.commands.LoadSecureDataCommand
import net.kibotu.jsbridge.commands.NavigationCommand
import net.kibotu.jsbridge.commands.NetworkStatusCommand
import net.kibotu.jsbridge.commands.OpenSettingsCommand
import net.kibotu.jsbridge.commands.RemoveSecureDataCommand
import net.kibotu.jsbridge.commands.RequestPermissionsCommand
import net.kibotu.jsbridge.commands.SaveSecureDataCommand
import net.kibotu.jsbridge.commands.ShowAlertCommand
import net.kibotu.jsbridge.commands.ShowToastCommand
import net.kibotu.jsbridge.commands.bottomnavigation.BottomNavigationCommand
import net.kibotu.jsbridge.commands.refresh.RefreshCommand
import net.kibotu.jsbridge.commands.systembars.SystemBarsCommand
import net.kibotu.jsbridge.commands.systembars.SystemBarsInfoCommand
import net.kibotu.jsbridge.commands.theme.ThemeChangedCommand
import net.kibotu.jsbridge.commands.topnavigation.TopNavigationCommand
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
 *     commands = DefaultCommands.all()
 * )
 * ```
 *
 * Commands that need the bridge (or its context) implement [BridgeAware]
 * and are wired automatically by [JavaScriptBridge.inject].
 */
object DefaultCommands {

    /** Returns all built-in commands. */
    fun all(): List<BridgeCommand> = listOf(
        DeviceInfoCommand(),
        NetworkStatusCommand(),
        SystemBarsCommand(),
        SystemBarsInfoCommand(),
        GetInsetsCommand(),
        HapticCommand(),
        RequestPermissionsCommand(),
        OpenSettingsCommand(),
        CopyToClipboardCommand(),
        NavigationCommand(),
        TopNavigationCommand(),
        BottomNavigationCommand(),
        ShowToastCommand(),
        ShowAlertCommand(),
        SaveSecureDataCommand(),
        LoadSecureDataCommand(),
        RemoveSecureDataCommand(),
        ThemeChangedCommand(),
        TrackEventCommand(),
        TrackScreenCommand(),
        RefreshCommand()
    )
}
