import SwiftUI
import WebKit
import Orchard
import JSBridge

/// SwiftUI wrapper for WKWebView with JavaScript bridge integration
struct WebViewContainer: UIViewControllerRepresentable {
    let url: URL
    let shouldRespectTopSafeArea: Bool
    let shouldRespectBottomSafeArea: Bool
    let onBridgeReady: (JavaScriptBridge) -> Void

    func makeUIViewController(context: Context) -> WebViewController {
        let controller = WebViewController()
        controller.url = url
        controller.shouldRespectTopSafeArea = shouldRespectTopSafeArea
        controller.shouldRespectBottomSafeArea = shouldRespectBottomSafeArea
        controller.onBridgeReady = onBridgeReady
        return controller
    }

    func updateUIViewController(_ uiViewController: WebViewController, context: Context) {
        if uiViewController.webView.url != url {
            let request = URLRequest(url: url)
            uiViewController.webView.load(request)
        }

        if uiViewController.shouldRespectTopSafeArea != shouldRespectTopSafeArea ||
           uiViewController.shouldRespectBottomSafeArea != shouldRespectBottomSafeArea {
            uiViewController.shouldRespectTopSafeArea = shouldRespectTopSafeArea
            uiViewController.shouldRespectBottomSafeArea = shouldRespectBottomSafeArea
            uiViewController.updateContentInsets()
        }
    }
}

/// WKWebView subclass that prevents its scroll view gesture recognizers
/// from capturing touches outside its visible bounds.
class BoundsRespectingWebView: WKWebView {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard self.bounds.contains(point) else { return nil }
        return super.hitTest(point, with: event)
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        return self.bounds.contains(point)
    }
}

/// UIViewController that hosts the WKWebView and manages the bridge
class WebViewController: UIViewController, WKNavigationDelegate, WindowFocusObserver {
    var webView: WKWebView!
    var bridge: JavaScriptBridge?
    var url: URL?
    var onBridgeReady: ((JavaScriptBridge) -> Void)?
    var shouldRespectTopSafeArea: Bool = false
    var shouldRespectBottomSafeArea: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()

        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true

        webView = BoundsRespectingWebView(frame: view.bounds, configuration: configuration)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.navigationDelegate = self

        view.addSubview(webView)

        let jsBridge = JavaScriptBridge(
            webView: webView,
            viewController: self,
            commands: DefaultCommands.all(
                viewController: self,
                webView: webView
            )
        )
        bridge = jsBridge
        onBridgeReady?(jsBridge)

        Orchard.v("[WebViewController] Bridge initialized")

        if let url = url {
            let request = URLRequest(url: url)
            webView.load(request)
        }
    }

    // MARK: - WindowFocusObserver

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        windowFocusDidAppear()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        windowFocusWillDisappear()
    }

    func onWindowFocusChanged(hasFocus: Bool) {
        if hasFocus {
            bridge?.sendToWeb(action: "lifecycle", content: ["event": "focused"])
            SafeAreaService.shared.pushToBridge(bridge)
        } else {
            bridge?.sendToWeb(action: "lifecycle", content: ["event": "defocused"])
        }
    }

    // MARK: - Safe Area

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateContentInsets()
    }

    func updateContentInsets() {
        webView.scrollView.contentInset = .zero
        webView.scrollView.scrollIndicatorInsets = .zero

        SafeAreaService.shared.pushToBridge(bridge)

        Orchard.v("[WebViewController] Updated content insets")
    }

    // MARK: - WKNavigationDelegate

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        Orchard.v("[WebView] Page loaded: \(webView.url?.absoluteString ?? "unknown")")
        updateContentInsets()
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        Orchard.e("[WebView] Navigation failed: \(error.localizedDescription)")
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        Orchard.e("[WebView] Provisional navigation failed: \(error.localizedDescription)")
    }
}
