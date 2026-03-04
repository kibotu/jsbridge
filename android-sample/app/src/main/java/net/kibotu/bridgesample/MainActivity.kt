package net.kibotu.bridgesample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import net.kibotu.bridgesample.bridge.JavaScriptBridge
import net.kibotu.bridgesample.bridge.SafeAreaService
import net.kibotu.bridgesample.bridge.commands.refresh.RefreshService
import net.kibotu.bridgesample.misc.weak
import net.kibotu.bridgesample.ui.Screen
import net.kibotu.bridgesample.ui.theme.BridgeSampleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var currentBridge: JavaScriptBridge? by weak()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

            BridgeSampleTheme(darkTheme = isDarkTheme) {
                LaunchedEffect(isDarkTheme) {
                    currentBridge?.sendToWeb(
                        "themeChanged",
                        mapOf("theme" to if (isDarkTheme) "dark" else "light")
                    )
                }

                Screen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onBridgeReady = { currentBridge = it }
                )
            }
        }

        lifecycleScope.launch {
            while (true) {
                delay(Random.nextLong(7000, 15000))
                currentBridge?.sendToWeb(
                    "onPushNotification",
                    mapOf(
                        "url" to "https://www.google.com",
                        "message" to "Lorem Ipsum"
                    )
                )
            }
        }

        lifecycleScope.launch {
            RefreshService.onRefresh.collect {
                Timber.v("refreshing $it")
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val event = if (hasFocus) "focused" else "defocused"
        currentBridge?.sendToWeb("lifecycle", mapOf("event" to event))
        if (hasFocus) {
            SafeAreaService.pushTobridge(currentBridge)
        }
    }
}

