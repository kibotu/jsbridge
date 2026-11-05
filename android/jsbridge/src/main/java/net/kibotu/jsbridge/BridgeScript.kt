package net.kibotu.jsbridge

import net.kibotu.jsbridge.commands.BridgeCommand
import org.json.JSONArray

/** Sorted, de-duplicated feature names the given commands expose to `jsbridge.supports()`. */
internal fun featureNames(commands: List<BridgeCommand>): List<String> =
    commands.flatMapTo(sortedSetOf()) { it.features }.toList()

/** Fills the `bridge.js` template placeholders. */
internal fun renderScript(template: String, bridgeName: String, commands: List<BridgeCommand>): String =
    template
        .replace("__BRIDGE_NAME__", bridgeName)
        .replace("__FEATURES__", JSONArray(featureNames(commands)).toString())
