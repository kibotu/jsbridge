package net.kibotu.bridgesample.bridge

import net.kibotu.bridgesample.bridge.commands.bottomnavigation.BottomNavigationCommand
import net.kibotu.bridgesample.bridge.commands.CheckNetworkStatusCommand
import net.kibotu.bridgesample.bridge.commands.GetInsetsCommand
import net.kibotu.bridgesample.bridge.commands.CopyToClipboardCommand
import net.kibotu.bridgesample.bridge.commands.DeviceInfoCommand
import net.kibotu.bridgesample.bridge.commands.HapticCommand
import net.kibotu.bridgesample.bridge.commands.LoadSecureDataCommand
import net.kibotu.bridgesample.bridge.commands.NavigationCommand
import net.kibotu.bridgesample.bridge.commands.OpenSettingsCommand
import net.kibotu.bridgesample.bridge.commands.OpenUrlCommand
import net.kibotu.bridgesample.bridge.commands.RemoveSecureDataCommand
import net.kibotu.bridgesample.bridge.commands.RequestPermissionsCommand
import net.kibotu.bridgesample.bridge.commands.SaveSecureDataCommand
import net.kibotu.bridgesample.bridge.commands.ShowAlertCommand
import net.kibotu.bridgesample.bridge.commands.ShowToastCommand
import net.kibotu.bridgesample.bridge.commands.systembars.SystemBarsCommand
import net.kibotu.bridgesample.bridge.commands.topnavigation.TopNavigationCommand
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import net.kibotu.bridgesample.bridge.commands.refresh.RefreshCommand
import timber.log.Timber

/**
 * Routes bridge messages to their corresponding command handlers using strategy pattern.
 *
 * **Why this exists:**
 * Separates message routing concerns from bridge communication logic. JavaScriptBridge
 * handles protocol/transport, while this handler focuses on command dispatch and execution.
 *
 * **Why strategy pattern:**
 * Each BridgeCommand is a self-contained handler for one action. This design:
 * - Makes adding new commands trivial (just add to list)
 * - Keeps commands independent and testable in isolation
 * - Follows Single Responsibility Principle at the command level
 * - Enables command reuse across different bridge implementations
 *
 * **Why command list approach:**
 * Simple list-based lookup is sufficient for ~20 commands and allows easy configuration.
 * Could be optimized to HashMap for 100+ commands, but current approach is more readable.
 *
 * **Error handling philosophy:**
 * Returns error responses instead of throwing exceptions, allowing web to handle failures
 * gracefully without breaking the bridge connection.
 */
class DefaultBridgeMessageHandler(
    private val getBridge: () -> JavaScriptBridge? = { null }
) : BridgeMessageHandler {

    private val commands = listOf(
        DeviceInfoCommand(),
        CheckNetworkStatusCommand(),
        SystemBarsCommand(),
        GetInsetsCommand(),
        HapticCommand(),
        RequestPermissionsCommand(),
        OpenSettingsCommand(),
        CopyToClipboardCommand(),
        OpenUrlCommand(),
        NavigationCommand(),
        TopNavigationCommand(getBridge),
        BottomNavigationCommand(getBridge),
        ShowToastCommand(),
        ShowAlertCommand(),
        SaveSecureDataCommand(),
        LoadSecureDataCommand(),
        RemoveSecureDataCommand(),
        RefreshCommand()
    )

    /**
     * Dispatches incoming bridge messages to registered command handlers.
     *
     * **Why linear search:**
     * With ~20 commands, linear search (O(n)) is faster than HashMap (hashing overhead).
     * Benchmarks show linear search is faster than HashMap for n < 50 items.
     *
     * **Why catch-all error handling:**
     * Safety net preventing crashes from unexpected command failures. Individual commands
     * should handle their own errors, but this prevents bridge breaking on bugs.
     *
     * **Why return error responses:**
     * Allows web to react to unknown actions (maybe feature not available in this version)
     * instead of silently failing, improving debuggability.
     *
     * @param action The action string from web (e.g., "deviceInfo", "showToast")
     * @param content Optional action parameters/payload from web
     * @return Response object (success/error) or null for fire-and-forget commands
     */
    override suspend fun handle(action: String, content: Any?): Any? {
        Timber.d("[handle] action=$action content=$content")

        return try {
            val command = commands.find { it.action == action }

            // Return error for unknown actions so web can detect missing features
            if (command == null) {
                Timber.w("[handle] Unknown action: $action")
                return BridgeResponseUtils.createErrorResponse(
                    "UNKNOWN_ACTION",
                    "Unknown action: $action"
                )
            }

            // Delegate to command handler - each command is responsible for its domain
            command.handle(content)
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse("INTERNAL_ERROR", e.message ?: "Unknown error")
        }
    }
}
