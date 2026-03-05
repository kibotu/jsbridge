package net.kibotu.bridgesample

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.kibotu.jsbridge.DefaultCommands
import net.kibotu.jsbridge.JavaScriptBridge

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    url: String,
    onBridgeReady: (JavaScriptBridge) -> Unit
) {
    val bridgeState = remember { mutableStateOf<JavaScriptBridge?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            bridgeState.value?.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webChromeClient = WebChromeClient()

                var bridgeRef: JavaScriptBridge? = null
                val bridge = JavaScriptBridge.inject(
                    webView = this,
                    commands = DefaultCommands.all(getBridge = { bridgeRef })
                )
                bridgeRef = bridge
                bridgeState.value = bridge
                onBridgeReady(bridge)
                loadUrl(url)
            }
        },
        update = { view ->
            if (view.url != url) {
                view.loadUrl(url)
            }
        }
    )
}
