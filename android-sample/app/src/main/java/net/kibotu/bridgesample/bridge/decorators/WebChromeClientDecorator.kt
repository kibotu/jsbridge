package net.kibotu.bridgesample.bridge.decorators

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Base decorator for [WebChromeClient] that delegates every callback to the wrapped [delegate].
 * Subclass and override only the callbacks you care about.
 */
open class WebChromeClientDecorator(
    protected val delegate: WebChromeClient?
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        delegate?.onProgressChanged(view, newProgress) ?: super.onProgressChanged(view, newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        delegate?.onReceivedTitle(view, title) ?: super.onReceivedTitle(view, title)
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        delegate?.onReceivedIcon(view, icon) ?: super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
        delegate?.onReceivedTouchIconUrl(view, url, precomposed)
            ?: super.onReceivedTouchIconUrl(view, url, precomposed)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean = delegate?.onShowFileChooser(webView, filePathCallback, fileChooserParams)
        ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)

    override fun onPermissionRequest(request: PermissionRequest?) {
        delegate?.onPermissionRequest(request) ?: super.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        delegate?.onPermissionRequestCanceled(request) ?: super.onPermissionRequestCanceled(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        delegate?.onGeolocationPermissionsShowPrompt(origin, callback)
            ?: super.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onGeolocationPermissionsHidePrompt() {
        delegate?.onGeolocationPermissionsHidePrompt() ?: super.onGeolocationPermissionsHidePrompt()
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean =
        delegate?.onJsAlert(view, url, message, result) ?: super.onJsAlert(view, url, message, result)

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean =
        delegate?.onJsConfirm(view, url, message, result) ?: super.onJsConfirm(view, url, message, result)

    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean =
        delegate?.onJsPrompt(view, url, message, defaultValue, result)
            ?: super.onJsPrompt(view, url, message, defaultValue, result)

    override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean =
        delegate?.onJsBeforeUnload(view, url, message, result) ?: super.onJsBeforeUnload(view, url, message, result)

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean =
        delegate?.onConsoleMessage(consoleMessage) ?: super.onConsoleMessage(consoleMessage)

    @Deprecated("Deprecated in Java")
    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
        @Suppress("DEPRECATION")
        delegate?.onConsoleMessage(message, lineNumber, sourceID)
            ?: super.onConsoleMessage(message, lineNumber, sourceID)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        delegate?.onShowCustomView(view, callback) ?: super.onShowCustomView(view, callback)
    }

    @Deprecated("Deprecated in Java")
    override fun onShowCustomView(view: View?, requestedOrientation: Int, callback: CustomViewCallback?) {
        @Suppress("DEPRECATION")
        delegate?.onShowCustomView(view, requestedOrientation, callback)
            ?: super.onShowCustomView(view, requestedOrientation, callback)
    }

    override fun onHideCustomView() {
        delegate?.onHideCustomView() ?: super.onHideCustomView()
    }

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean =
        delegate?.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            ?: super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)

    override fun onCloseWindow(window: WebView?) {
        delegate?.onCloseWindow(window) ?: super.onCloseWindow(window)
    }

    override fun onRequestFocus(view: WebView?) {
        delegate?.onRequestFocus(view) ?: super.onRequestFocus(view)
    }

    override fun getDefaultVideoPoster(): Bitmap? =
        delegate?.defaultVideoPoster ?: super.getDefaultVideoPoster()

    override fun getVideoLoadingProgressView(): View? =
        delegate?.videoLoadingProgressView ?: super.getVideoLoadingProgressView()

    override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
        delegate?.getVisitedHistory(callback) ?: super.getVisitedHistory(callback)
    }

    @Deprecated("Deprecated in Java")
    override fun onExceededDatabaseQuota(
        url: String?, databaseIdentifier: String?, quota: Long,
        estimatedDatabaseSize: Long, totalQuota: Long, quotaUpdater: WebStorage.QuotaUpdater?
    ) {
        @Suppress("DEPRECATION")
        delegate?.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
            ?: super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
    }
}
