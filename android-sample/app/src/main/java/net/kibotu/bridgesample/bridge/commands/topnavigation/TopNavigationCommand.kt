package net.kibotu.bridgesample.bridge.commands.topnavigation

import net.kibotu.bridgesample.bridge.commands.utils.BridgeParsingUtils
import net.kibotu.bridgesample.bridge.commands.utils.BridgeResponseUtils
import net.kibotu.bridgesample.bridge.commands.BridgeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Configures app's top toolbar/ActionBar appearance from web content.
 *
 * **Why web needs this:**
 * Native toolbar is outside WebView's DOM - web cannot control it directly.
 * This enables web to:
 * - Show/hide toolbar based on scroll or content type
 * - Set page-specific titles
 * - Control back button visibility (navigation affordance)
 * - Customize branding (logo vs title)
 *
 * **Why so many options:**
 * Different web pages have different toolbar needs:
 * - Landing pages: Logo, no back button
 * - Detail pages: Title with back button
 * - Full-screen content: Hidden toolbar
 * - Branded sections: Logo + profile widget
 *
 * **Why drawBehindStatusBar when hidden:**
 * When toolbar hides, content should expand into status bar area (immersive).
 * Prevents awkward gap at top of screen.
 */
class TopNavigationCommand : BridgeCommand {

    override val action = "topNavigation"

    override suspend fun handle(content: Any?): JSONObject = withContext(Dispatchers.Main) {
        try {
            val isVisible = BridgeParsingUtils.parseBoolean(content, "isVisible")
            val title = BridgeParsingUtils.parseString(content, "title")
            val showUpArrow = BridgeParsingUtils.parseBoolean(content, "showUpArrow")
            val showDivider = BridgeParsingUtils.parseBoolean(content, "showDivider")
            val showLogo = BridgeParsingUtils.parseBoolean(content, "showLogo")
            val showProfileIconWidget =
                BridgeParsingUtils.parseBoolean(content, "showProfileIconWidget")

            Timber.i("[handle] isVisible=$isVisible title=$title showUpArrow=$showUpArrow")

            TopNavigationService.applyConfig(
                TopNavigationConfig(
                    isVisible = isVisible == true,
                    title = title.takeIf { it.isNotEmpty() },
                    showUpArrow = showUpArrow == true,
                    showDivider = showDivider == true,
                    showLogo = showLogo == true,
                    showProfileIconWidget = showProfileIconWidget == true,
                    drawBehindStatusBar = isVisible == false
                )
            )
            BridgeResponseUtils.createSuccessResponse()
        } catch (e: Exception) {
            Timber.e(e)
            BridgeResponseUtils.createErrorResponse(
                "TOP_NAVIGATION_FAILED",
                e.message ?: "Failed to configure top navigation"
            )
        }
    }
}

data class TopNavigationConfig(
    val isVisible: Boolean = true,
    val showUpArrow: Boolean = false,
    val showDebugIcon: Boolean = true,
    val showDivider: Boolean = true,
    val showLogo: Boolean = false,
    val showProfileIconWidget: Boolean = false,
    val title: String? = null,
    val drawBehindStatusBar: Boolean = false
)