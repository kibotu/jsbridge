import Foundation
import WebKit
import Orchard

/// Weak proxy to avoid the WKUserContentController retain cycle.
///
/// `WKUserContentController.add(_:name:)` retains its delegate strongly.
/// Without this proxy, the bridge and the WebView's content controller form a retain cycle:
/// contentController -> bridge -> (weak) webView -> configuration -> contentController.
@MainActor
private final class LeakAvoider: NSObject, WKScriptMessageHandler {
    weak var delegate: (any WKScriptMessageHandler)?

    init(delegate: any WKScriptMessageHandler) {
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

/// Main JavaScript bridge coordinator for WKWebView <-> native communication.
///
/// Configurable bridge name (default: `jsbridge`). The same name is used for:
/// - The WKScriptMessageHandler registration
/// - The `window.<name>` public JS API
/// - The global callback names (`window.__<name>_handleResponse`, etc.)
///
/// Commands are passed in at init time -- they are **not** auto-registered.
/// Use `DefaultCommands.all()` for a convenient all-in-one setup.
///
/// Multiple bridges can coexist on the same WKWebView as long as they
/// use different names.
@MainActor
public final class JavaScriptBridge: NSObject, WKScriptMessageHandler {
    private var commands: [any BridgeCommand] = []
    public private(set) weak var webView: WKWebView?
    public private(set) weak var viewController: UIViewController?
    private var bridgeScriptInjected = false

    public let name: String

    private var callbackResponse: String { "__\(name)_handleResponse" }
    private var callbackMessage: String { "__\(name)_handleNativeMessage" }

    public static let defaultBridgeName = "jsbridge"

    /// Creates and configures the bridge.
    ///
    /// Commands implementing ``BridgeAware`` receive the bridge reference
    /// automatically -- no manual wiring needed.
    ///
    /// - Parameters:
    ///   - webView: The WKWebView to bridge with.
    ///   - viewController: The hosting view controller (used by UI commands like alerts/toasts).
    ///   - bridgeName: The name exposed to JavaScript (default: `jsbridge`).
    ///   - commands: The commands this bridge responds to.
    public init(
        webView: WKWebView,
        viewController: UIViewController,
        bridgeName: String = defaultBridgeName,
        commands: [any BridgeCommand]
    ) {
        self.name = bridgeName
        self.webView = webView
        self.viewController = viewController
        super.init()

        for command in commands {
            register(command: command)
            if let aware = command as? BridgeAware {
                aware.bridge = self
            }
        }

        setupMessageHandler()
        injectBridgeScript()
        #if DEBUG
        injectConsoleLogInterceptor()
        #endif
    }

    deinit {
        let name = self.name
        let webView = self.webView
        Task { @MainActor in
            webView?.configuration.userContentController.removeScriptMessageHandler(forName: name)
        }
    }

    // MARK: - Setup

    private func setupMessageHandler() {
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: name)
        webView?.configuration.userContentController.add(LeakAvoider(delegate: self), name: name)
    }

    private func register(command: any BridgeCommand) {
        commands.append(command)
        Orchard.v("[Bridge] Registered command for action: \(command.action)")
    }

    private func command(for action: String) -> (any BridgeCommand)? {
        commands.first { $0.action == action }
    }

    /// Injects the unified bridge JavaScript into the WebView at document start.
    /// Reads bridge.js from the bundle resource and replaces template variables.
    /// Guards against duplicate injection to prevent WKUserScript accumulation.
    public func injectBridgeScript() {
        guard !bridgeScriptInjected else {
            Orchard.v("[Bridge] Bridge script already injected, skipping")
            return
        }

        let script = loadBridgeScript()
        let userScript = WKUserScript(
            source: script,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )

        webView?.configuration.userContentController.addUserScript(userScript)
        bridgeScriptInjected = true
        Orchard.v("[Bridge] Bridge script injected (name=\(name))")
    }

    private func loadBridgeScript() -> String {
        guard let url = Bundle.module.url(forResource: "bridge", withExtension: "js"),
              let rawScript = try? String(contentsOf: url, encoding: .utf8) else {
            Orchard.e("[Bridge] Failed to load bridge.js from bundle")
            return ""
        }

        return BridgeScript.render(rawScript, name: name, commands: commands)
    }

    // MARK: - WKScriptMessageHandler

    public func userContentController(
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

        let action = message.data.action
        let content = message.data.content?.compactMapValues { $0.value is NSNull ? nil : $0.value }

        Orchard.v("[Bridge] Received action: \(action)")

        guard let command = command(for: action) else {
            Orchard.w("[Bridge] Unknown action: \(action)")
            sendError(id: message.id, error: .unknownAction(action))
            return
        }

        let messageId = message.id
        Task { [weak self] in
            do {
                let responseData = try await command.handle(content: content)
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

    private func sendResponseJSON(_ response: [String: Any]) {
        guard let jsonData = try? JSONSerialization.data(withJSONObject: response),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            Orchard.e("[Bridge] Failed to encode response")
            return
        }

        let script = "window.\(callbackResponse) && window.\(callbackResponse)(\(jsonString));"

        webView?.evaluateJavaScript(script) { _, error in
            if let error = error {
                Orchard.e("[Bridge] Failed to send response: \(error)")
            }
        }
    }

    // MARK: - Native to Web Communication

    /// Send a message from native to web (fire-and-forget).
    public func sendToWeb(action: String, content: [String: Any]? = nil) {
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

        webView?.evaluateJavaScript(script) { _, error in
            if let error = error {
                Orchard.e("[Bridge] Failed to send message to web: \(error)")
            }
        }
    }

    // MARK: - Safe Area CSS Injection

    /// Injects/updates CSS custom properties for safe area insets.
    /// Call whenever bars change visibility, on rotation, or keyboard show/hide.
    public func updateSafeAreaCSS(
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

        webView?.evaluateJavaScript(js) { _, error in
            if let error = error {
                Orchard.e("[Bridge] Failed to inject safe area CSS: \(error)")
            }
        }
    }

    // MARK: - Lifecycle Events

    public func notifyLifecycleEvent(_ event: String) {
        sendToWeb(action: "lifecycle", content: ["event": event])
    }
    
    // MARK: Logging Web Console
    
    private static let consoleHandlerName = "nativeConsoleLog"
    
    // swiftlint:disable function_body_length
    func injectConsoleLogInterceptor() {
        guard let webView = webView else { return }
        
        let js = """
        (function() {
            if (window.__nativeConsoleHooked) return;
            window.__nativeConsoleHooked = true;
            
            function getCallerLocation() {
                try {
                    var stack = new Error().stack || '';
                    var lines = stack.split('\\n');
                    // JSC format: "functionName@file:line:col" or "@file:line:col"
                    for (var i = 0; i < lines.length; i++) {
                        var line = lines[i];
                        // Skip internal/hooked frames
                        if (!line || line.indexOf('getCallerLocation') !== -1 || line.indexOf('__nativeConsoleHooked') !== -1) continue;
                        // JSC: "someFunc@http://localhost:8080/app.js:42:10"
                        // JSC anonymous: "@http://localhost:8080/app.js:42:10"
                        var match = line.match(/@(.+):(\\d+):\\d+$/);
                        if (match) {
                            var file = match[1].split('/').pop();
                            return file + ':' + match[2];
                        }
                    }
                } catch(e) {}
                return '';
            }
            
            var levels = ['log', 'debug', 'info', 'warn', 'error'];
            levels.forEach(function(level) {
                var original = console[level];
                console[level] = function() {
                    var args = Array.prototype.slice.call(arguments).map(function(a) {
                        if (a instanceof Error) {
                            return a.message + (a.stack ? '\\n' + a.stack : '');
                        }
                        try { return (typeof a === 'object') ? JSON.stringify(a, null, 2) : String(a); }
                        catch(e) { return String(a); }
                    });
                    
                    window.webkit.messageHandlers.\(Self.consoleHandlerName).postMessage({
                        level: level,
                        message: args.join(' '),
                        location: getCallerLocation()
                    });
                    original.apply(console, arguments);
                };
            });
        })();
        """
        
        let script = WKUserScript(source: js, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        webView.configuration.userContentController.addUserScript(script)
        webView.configuration.userContentController.add(self, name: Self.consoleHandlerName)
    }
    
    func console(_ message: WKScriptMessage) {
        guard message.name == Self.consoleHandlerName,
              let body = message.body as? [String: Any],
              let level = body["level"] as? String,
              let text = body["message"] as? String else { return }
        
        let location = (body["location"] as? String).flatMap { $0.isEmpty ? nil : $0 }
        let prefix = location.map { "[\($0)] " } ?? ""
        let output = prefix + text
        
        Orchard.loggers.compactMap({ $0 as? ConsoleLogger }).first?.showInvocation = false

        Orchard.tag("Console")
        
        switch level {
        case "error": Orchard.e(output)
        case "warn":  Orchard.w(output)
        case "info":  Orchard.i(output)
        case "debug": Orchard.d(output)
        default:      Orchard.v(output)
        }
        
        Orchard.loggers.compactMap({ $0 as? ConsoleLogger }).first?.showInvocation = true
    }
}
