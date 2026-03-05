package net.kibotu.bridgesample.bridge.commands.tracking

import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import net.kibotu.bridgesample.bridge.commands.utils.BridgeParsingUtils
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import timber.log.Timber

/**
 * Tracks screen views (fire-and-forget).
 *
 * Separate from trackEvent because many analytics services treat page views
 * differently from events, and following Firebase Analytics conventions for
 * screen tracking with `screen_view` event name.
 *
 * Fire-and-forget: returns null so no response is sent back to web.
 */
class TrackScreenCommand : BridgeCommand {

    override val action = "trackScreen"

    override suspend fun handle(content: Any?): Any? {
        val screenName = BridgeParsingUtils.parseString(content, "screenName")
        if (screenName.isEmpty()) {
            return BridgeResponseUtils.createErrorResponse(
                "INVALID_PARAMETER",
                "Missing 'screenName' parameter"
            )
        }

        val screenClass = BridgeParsingUtils.parseString(content, "screenClass")

        Timber.i("[Bridge] Track screen: $screenName, class: $screenClass")

        // TODO: Forward to your analytics service (e.g. Firebase Analytics)

        return null
    }
}
