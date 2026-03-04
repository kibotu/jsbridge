package net.kibotu.bridgesample

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.kibotu.bridgesample.bridge.DefaultBridgeMessageHandler
import net.kibotu.bridgesample.bridge.JavaScriptBridge

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    url: String,
    onBridgeReady: (JavaScriptBridge) -> Unit
) {
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
                val handler = DefaultBridgeMessageHandler(getBridge = { bridgeRef })
                val bridge = JavaScriptBridge.inject(this, handler)
                bridgeRef = bridge
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