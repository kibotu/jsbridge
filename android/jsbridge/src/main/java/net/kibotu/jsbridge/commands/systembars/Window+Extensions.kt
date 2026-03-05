package net.kibotu.jsbridge.commands.systembars

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

var Window.showSystemStatusBar: Boolean
    get() {
        throw NotImplementedError()
    }
    set(value) {
        with(WindowCompat.getInsetsController(this, this.decorView)) {
            if (value) {
                show(WindowInsetsCompat.Type.statusBars())
            } else {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

var Window.showSystemNavigationBar: Boolean
    get() {
        throw NotImplementedError()
    }
    set(value) {
        with(WindowCompat.getInsetsController(this, this.decorView)) {
            if (value) {
                show(WindowInsetsCompat.Type.navigationBars())
            } else {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

var Window.isLightStatusBar
    get() = WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars
    set(enabled) {
        WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = enabled
    }

var Window.isLightNavigationBar
    get() = WindowCompat.getInsetsController(this, decorView).isAppearanceLightNavigationBars
    set(enabled) {
        WindowCompat.getInsetsController(this, decorView).isAppearanceLightNavigationBars = enabled
    }