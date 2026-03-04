package net.kibotu.bridgesample.bridge

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.florent37.application.provider.ActivityProvider
import net.kibotu.bridgesample.bridge.commands.bottomnavigation.BottomNavigationService
import net.kibotu.bridgesample.bridge.commands.topnavigation.TopNavigationService
import timber.log.Timber

/**
 * Computes and pushes safe area CSS custom properties to the WebView.
 *
 * Called from multiple sites following the provider pattern:
 * - onPageFinished (initial load + navigations)
 * - TopNavigationCommand (after toggling top bar)
 * - BottomNavigationCommand (after toggling bottom bar)
 * - onWindowFocusChanged(true) (returning from another screen/app)
 */
object SafeAreaService {

    var topBarHeightDp: Int = 0
    var bottomBarHeightDp: Int = 0

    fun pushTobridge(bridge: JavaScriptBridge?) {
        if (bridge == null) return

        val activity = ActivityProvider.currentActivity ?: return
        val density = activity.resources.displayMetrics.density
        val rootView: View = activity.window.decorView

        val insets = ViewCompat.getRootWindowInsets(rootView)
        val statusBarPx = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val systemNavPx = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0

        val statusBarDp = pxToDp(statusBarPx, density)
        val systemNavDp = pxToDp(systemNavPx, density)

        val topNavConfig = TopNavigationService.currentConfig()
        val isBottomBarVisible = BottomNavigationService.currentVisibility()

        // When toolbar is visible, Scaffold already positions the WebView below it -- CSS inset = 0.
        // When toolbar is hidden, Scaffold has no top bar and contentWindowInsets is 0,
        // so the WebView extends under the status bar -- CSS needs the status bar height.
        val effectiveTopDp = if (topNavConfig.isVisible) 0 else statusBarDp
        val effectiveBottomDp = if (isBottomBarVisible) bottomBarHeightDp else 0

        Timber.d(
            "[SafeAreaService] top=%ddp (statusBar=%d topNav=%d visible=%b) bottom=%ddp (bottomNav=%d visible=%b) systemNav=%ddp",
            effectiveTopDp, statusBarDp, topBarHeightDp, topNavConfig.isVisible,
            effectiveBottomDp, bottomBarHeightDp, isBottomBarVisible,
            systemNavDp
        )

        bridge.updateSafeAreaCSS(
            insetTop = effectiveTopDp,
            insetBottom = effectiveBottomDp,
            statusBarHeight = statusBarDp,
            topNavHeight = if (topNavConfig.isVisible) topBarHeightDp else 0,
            bottomNavHeight = if (isBottomBarVisible) bottomBarHeightDp else 0,
            systemNavHeight = systemNavDp
        )
    }

    private fun pxToDp(px: Int, density: Float): Int = (px / density).toInt()
}
