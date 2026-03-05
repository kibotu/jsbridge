package net.kibotu.jsbridge.decorators

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import net.kibotu.jsbridge.JavaScriptBridge
import net.kibotu.jsbridge.JavaScriptBridge.Companion.bridge
import net.kibotu.jsbridge.SafeAreaService
import timber.log.Timber

/**
 * WebViewClient decorator that automatically injects the JavaScript bridge on page start
 * and pushes safe area CSS on page finish.
 *
 * Wraps whatever WebViewClient the WebView already has, preserving its behavior.
 * When multiple bridges are registered, injects scripts for the default bridge.
 */
class BridgeWebViewClient(delegate: WebViewClient?) : WebViewClientDecorator(delegate) {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val bridge = view?.bridge() ?: return
        bridge.injectBridgeScript()
        Timber.d("[BridgeWebViewClient] Bridge injected for: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val bridge = view?.bridge() ?: return
        SafeAreaService.pushTobridge(bridge)
    }
}
