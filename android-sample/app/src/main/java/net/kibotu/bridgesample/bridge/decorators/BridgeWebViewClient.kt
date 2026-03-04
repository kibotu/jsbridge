package net.kibotu.bridgesample.bridge.decorators

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import net.kibotu.bridgesample.bridge.JavaScriptBridge
import net.kibotu.bridgesample.bridge.SafeAreaService
import timber.log.Timber

/**
 * WebViewClient decorator that automatically injects the JavaScript bridge on page start
 * and pushes safe area CSS on page finish.
 *
 * Wraps whatever WebViewClient the WebView already has, preserving its behavior.
 * The bridge instance is stored in [WebView.tag] for retrieval.
 */
class BridgeWebViewClient(delegate: WebViewClient?) : WebViewClientDecorator(delegate) {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val bridge = view?.tag as? JavaScriptBridge ?: return
        bridge.injectBridgeScript()
        Timber.d("[BridgeWebViewClient] Bridge injected for: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val bridge = view?.tag as? JavaScriptBridge ?: return
        SafeAreaService.pushTobridge(bridge)
    }
}
