package net.kibotu.bridgesample

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.kibotu.bridgesample.bridge.DefaultBridgeMessageHandler
import net.kibotu.bridgesample.bridge.JavaScriptBridge
import timber.log.Timber

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
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Timber.d("WebView loaded: $url")
                        // Ensure bridge script is injected after page load
                        (view?.tag as? JavaScriptBridge)?.injectBridgeScript()
                    }
                }

                // Attach bridge
                val bridge = JavaScriptBridge(this, DefaultBridgeMessageHandler())
                addJavascriptInterface(bridge, JavaScriptBridge.DEFAULT_BRIDGE_NAME)
                tag = bridge
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