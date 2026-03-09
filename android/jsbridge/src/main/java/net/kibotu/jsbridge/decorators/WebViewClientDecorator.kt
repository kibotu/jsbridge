package net.kibotu.jsbridge.decorators

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/**
 * Base decorator for [WebViewClient] that delegates every callback to the wrapped [delegate].
 *
 * Android's WebView only allows one WebViewClient at a time. This decorator lets
 * the bridge layer add behavior (script injection, safe area updates) without
 * replacing the app's own WebViewClient. Subclass and override only what you need.
 *
 * @see BridgeWebViewClient
 */
open class WebViewClientDecorator(
    protected val delegate: WebViewClient?
) : WebViewClient() {

    @RequiresApi(24)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        delegate?.shouldOverrideUrlLoading(view, request) ?: super.shouldOverrideUrlLoading(view, request)

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        @Suppress("DEPRECATION")
        return delegate?.shouldOverrideUrlLoading(view, url) ?: super.shouldOverrideUrlLoading(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        delegate?.onPageStarted(view, url, favicon) ?: super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        delegate?.onPageFinished(view, url) ?: super.onPageFinished(view, url)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        delegate?.onLoadResource(view, url) ?: super.onLoadResource(view, url)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        delegate?.onPageCommitVisible(view, url) ?: super.onPageCommitVisible(view, url)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
        delegate?.shouldInterceptRequest(view, request) ?: super.shouldInterceptRequest(view, request)

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        @Suppress("DEPRECATION")
        return delegate?.shouldInterceptRequest(view, url) ?: super.shouldInterceptRequest(view, url)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        delegate?.onReceivedError(view, request, error) ?: super.onReceivedError(view, request, error)
    }

    @Deprecated("Deprecated in Java")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        @Suppress("DEPRECATION")
        delegate?.onReceivedError(view, errorCode, description, failingUrl)
            ?: super.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        delegate?.onReceivedHttpError(view, request, errorResponse)
            ?: super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        delegate?.onReceivedSslError(view, handler, error) ?: super.onReceivedSslError(view, handler, error)
    }

    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        delegate?.onReceivedClientCertRequest(view, request) ?: super.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        delegate?.onReceivedHttpAuthRequest(view, handler, host, realm)
            ?: super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
        delegate?.onReceivedLoginRequest(view, realm, account, args)
            ?: super.onReceivedLoginRequest(view, realm, account, args)
    }

    @Deprecated("Deprecated in Java")
    override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
        @Suppress("DEPRECATION")
        delegate?.onTooManyRedirects(view, cancelMsg, continueMsg)
            ?: super.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    @Deprecated("Deprecated in Java")
    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        @Suppress("DEPRECATION")
        delegate?.onFormResubmission(view, dontResend, resend) ?: super.onFormResubmission(view, dontResend, resend)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        delegate?.doUpdateVisitedHistory(view, url, isReload) ?: super.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        delegate?.onScaleChanged(view, oldScale, newScale) ?: super.onScaleChanged(view, oldScale, newScale)
    }

    override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean =
        delegate?.shouldOverrideKeyEvent(view, event) ?: super.shouldOverrideKeyEvent(view, event)

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        delegate?.onUnhandledKeyEvent(view, event) ?: super.onUnhandledKeyEvent(view, event)
    }

    @RequiresApi(27)
    override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
        delegate?.onSafeBrowsingHit(view, request, threatType, callback)
            ?: super.onSafeBrowsingHit(view, request, threatType, callback)
    }

    @RequiresApi(26)
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean =
        delegate?.onRenderProcessGone(view, detail) ?: super.onRenderProcessGone(view, detail)
}
