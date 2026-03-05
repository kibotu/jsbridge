import Foundation
import WebKit
import Orchard

/// Weak proxy to avoid the WKUserContentController retain cycle.
///
/// `WKUserContentController.add(_:name:)` retains its delegate strongly.
/// Without this proxy, the bridge and the WebView's content controller form a retain cycle:
/// contentController -> bridge -> (weak) webView -> configuration -> contentController.
private class LeakAvoider: NSObject, WKScriptMessageHandler {
    weak var delegate: WKScriptMessageHandler?

    init(delegate: WKScriptMessageHandler) {
        self.delegate = delegate
        super.init()
    }

    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        delegate?.userContentController(userContentController, didReceive: message)
    }
}

/// Main JavaScript bridge coordinator for WKWebView ↔ native communication.
///
/// Configurable bridge name (default: `jsbridge`). The same name is used for:
/// - The WKScriptMessageHandler registration
/// - The `window.<name>` public JS API
/// - The global callback names (`window.__<name>_handleResponse`, etc.)
///
/// Uses a handler registry (strategy pattern) to route actions.
class JavaScriptBridge: NSObject, WKScriptMessageHandler {
    private var handlers: [BridgeCommand] = []
    private weak var webView: WKWebView?
    private weak var viewController: UIViewController?
    private var bridgeScriptInjected = false

    let name: String

    private let schemaVersion: Int = 1

    private var callbackResponse: String { "__\(name)_handleResponse" }
    private var callbackMessage: String { "__\(name)_handleNativeMessage" }

    static let defaultBridgeName = "jsbridge"

    init(webView: WKWebView, viewController: UIViewController, bridgeName: String = defaultBridgeName) {
        self.name = bridgeName
        self.webView = webView
        self.viewController = viewController
        super.init()

        setupMessageHandler()
        registerCommandHandlers()
        injectBridgeScript()
    }

    deinit {
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: name)
    }

    // MARK: - Setup

    private func setupMessageHandler() {
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: name)
        webView?.configuration.userContentController.add(LeakAvoider(delegate: self), name: name)
    }

    private func registerCommandHandlers() {
        // Device & System
        register(handler: DeviceInfoHandler())
        register(handler: NetworkStatusHandler())
        register(handler: OpenSettingsHandler())

        // UI
        register(handler: ShowToastHandler(viewController: viewController))
        register(handler: ShowAlertHandler(viewController: viewController))

        // Navigation
        register(handler: TopNavigationHandler(bridge: self))
        register(handler: BottomNavigationHandler(bridge: self))
        register(handler: NavigationHandler(viewController: viewController, webView: webView))
        register(handler: OpenUrlHandler())

        // System
        register(handler: SystemBarsHandler())
        register(handler: GetInsetsHandler(viewController: viewController))
        register(handler: HapticHandler())
        register(handler: CopyToClipboardHandler())
        register(handler: RequestPermissionsHandler())

        // Storage
        register(handler: SaveSecureDataHandler())
        register(handler: LoadSecureDataHandler())
        register(handler: RemoveSecureDataHandler())

        // Analytics
        register(handler: TrackEventHandler())
        register(handler: TrackScreenHandler())

        // Refresh
        register(handler: RefreshHandler())
    }

    private func register(handler: BridgeCommand) {
        handlers.append(handler)
        Orchard.v("[Bridge] Registered handler for action: \(handler.actionName)")
    }

    private func handler(for action: String) -> BridgeCommand? {
        return handlers.first { $0.actionName == action }
    }

    private func isVersionSupported(_ version: Int) -> Bool {
        return version <= schemaVersion
    }

    /// Injects the unified bridge JavaScript into the WebView at document start.
    /// Guards against duplicate injection to prevent WKUserScript accumulation.
    func injectBridgeScript() {
        guard !bridgeScriptInjected else {
            Orchard.v("[Bridge] Bridge script already injected, skipping")
            return
        }

        let script = JavaScriptBridgeScript.source(bridgeName: name, schemaVersion: schemaVersion)
        let userScript = WKUserScript(
            source: script,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )

        webView?.configuration.userContentController.addUserScript(userScript)
        bridgeScriptInjected = true
        Orchard.v("[Bridge] Bridge script injected (name=\(name), schema v\(schemaVersion))")
    }

    // MARK: - WKScriptMessageHandler

    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        guard message.name == name else { return }

        guard let body = message.body as? String else {
            Orchard.w("[Bridge] Received invalid message body")
            return
        }

        handleMessage(body)
    }

    // MARK: - Message Handling

    private func handleMessage(_ messageString: String) {
        guard let data = messageString.data(using: .utf8) else {
            Orchard.e("[Bridge] Could not convert message to data")
            return
        }

        let decoder = JSONDecoder()

        guard let message = try? decoder.decode(JavaScriptBridgeMessage.self, from: data) else {
            Orchard.e("[Bridge] Could not decode message: \(messageString)")
            let rawId = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["id"] as? String
            if let rawId = rawId {
                sendError(id: rawId, error: .invalidMessage)
            }
            return
        }

        if !isVersionSupported(message.version) {
            Orchard.w("[Bridge] Silently ignoring unsupported version: \(message.version)")
            return
        }

        let action = message.data.action
        let content = message.data.content?.compactMapValues { $0.value is NSNull ? nil : $0.value }

        Orchard.v("[Bridge] Received action: \(action)")

        guard let handler = handler(for: action) else {
            Orchard.w("[Bridge] Unknown action: \(action)")
            sendError(id: message.id, error: .unknownAction(action))
            return
        }

        let messageId = message.id
        Task { [weak self] in
            do {
                let responseData = try await handler.handle(content: content)
                self?.sendSuccess(id: messageId, data: responseData)
            } catch let error as BridgeError {
                self?.sendError(id: messageId, error: error)
            } catch {
                self?.sendError(id: messageId, error: .internalError(error.localizedDescription))
            }
        }
    }

    // MARK: - Sending Responses (unified format: { id, data?, error? })

    private func sendSuccess(id: String, data: [String: Any]?) {
        var response: [String: Any] = ["id": id]
        if let data = data {
            response["data"] = data
        } else {
            response["data"] = [String: Any]()
        }
        sendResponseJSON(response)
    }

    private func sendError(id: String, error: BridgeError) {
        let response: [String: Any] = [
            "id": id,
            "error": [
                "code": error.code,
                "message": error.message
            ]
        ]
        sendResponseJSON(response)
    }

    /// Serializes the response dict and calls the unified JS callback.
    private func sendResponseJSON(_ response: [String: Any]) {
        guard let jsonData = try? JSONSerialization.data(withJSONObject: response),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            Orchard.e("[Bridge] Failed to encode response")
            return
        }

        let script = "window.\(callbackResponse) && window.\(callbackResponse)(\(jsonString));"

        DispatchQueue.main.async { [weak self] in
            self?.webView?.evaluateJavaScript(script) { _, error in
                if let error = error {
                    Orchard.e("[Bridge] Failed to send response: \(error)")
                }
            }
        }
    }

    // MARK: - Native to Web Communication

    /// Send a message from native to web (fire-and-forget).
    func sendToWeb(action: String, content: [String: Any]? = nil) {
        let message: [String: Any] = [
            "data": [
                "action": action,
                "content": content ?? [:]
            ]
        ]

        guard let jsonData = try? JSONSerialization.data(withJSONObject: message),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            Orchard.e("[Bridge] Failed to serialize message to web")
            return
        }

        let script = "window.\(callbackMessage) && window.\(callbackMessage)(\(jsonString));"

        DispatchQueue.main.async { [weak self] in
            self?.webView?.evaluateJavaScript(script) { _, error in
                if let error = error {
                    Orchard.e("[Bridge] Failed to send message to web: \(error)")
                }
            }
        }
    }

    // MARK: - Safe Area CSS Injection

    /// Injects/updates CSS custom properties for safe area insets.
    /// Call whenever bars change visibility, on rotation, or keyboard show/hide.
    func updateSafeAreaCSS(
        insetTop: CGFloat = 0,
        insetBottom: CGFloat = 0,
        insetLeft: CGFloat = 0,
        insetRight: CGFloat = 0,
        statusBarHeight: CGFloat = 0,
        topNavHeight: CGFloat = 0,
        bottomNavHeight: CGFloat = 0,
        systemNavHeight: CGFloat = 0
    ) {
        let js = """
        (function() {
            var el = document.documentElement;
            el.style.setProperty('--bridge-inset-top', '\(Int(insetTop))px');
            el.style.setProperty('--bridge-inset-bottom', '\(Int(insetBottom))px');
            el.style.setProperty('--bridge-inset-left', '\(Int(insetLeft))px');
            el.style.setProperty('--bridge-inset-right', '\(Int(insetRight))px');
            el.style.setProperty('--bridge-status-bar', '\(Int(statusBarHeight))px');
            el.style.setProperty('--bridge-top-nav', '\(Int(topNavHeight))px');
            el.style.setProperty('--bridge-bottom-nav', '\(Int(bottomNavHeight))px');
            el.style.setProperty('--bridge-system-nav', '\(Int(systemNavHeight))px');
        })();
        """

        DispatchQueue.main.async { [weak self] in
            self?.webView?.evaluateJavaScript(js) { _, error in
                if let error = error {
                    Orchard.e("[Bridge] Failed to inject safe area CSS: \(error)")
                }
            }
        }
    }

    // MARK: - Lifecycle Events

    func notifyLifecycleEvent(_ event: String) {
        sendToWeb(action: "lifecycle", content: ["event": event])
    }
}
