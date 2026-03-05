import Foundation
import UIKit
import WebKit
import Orchard

/// Handles multiple navigation patterns: back navigation, internal/external URLs.
///
/// **Why combined command:**
/// Navigation actions are mutually exclusive - user does one at a time. Single command
/// simplifies web API (one call instead of multiple) for common navigation patterns.
///
/// **Why goBack:**
/// Allows web to trigger native back navigation. Priority order:
/// 1. WebView history (if available)
/// 2. Navigation controller pop (if in a stack)
/// 3. Dismiss view controller (if presented modally)
/// 4. Exit app (as last resort on root view controller)
///
/// **Why external option:**
/// Some URLs should open in browser (privacy policies, external sites) to make
/// clear they're leaving the app. External prevents deep link interception.
///
/// **Thread Safety:**
/// All UIKit operations must run on the main thread, hence the DispatchQueue.main.async
class NavigationHandler: BridgeCommand {
    let actionName = "navigation"
    
    weak var viewController: UIViewController?
    weak var webView: WKWebView?
    
    init(viewController: UIViewController?, webView: WKWebView? = nil) {
        self.viewController = viewController
        self.webView = webView
    }
    
    @MainActor
    func handle(content: [String: Any]?) async throws -> [String: Any]? {
        let urlString = content?["url"] as? String ?? ""
        let isExternal = content?["external"] as? Bool ?? false
        let goBack = content?["goBack"] as? Bool ?? false
        
        Orchard.v("[NavigationHandler] url=\(urlString) external=\(isExternal) goBack=\(goBack)")
        
        if goBack {
            if let webView = webView, webView.canGoBack {
                webView.goBack()
                Orchard.v("[NavigationHandler] Navigated back in WebView history")
                return nil
            }
            
            if let navigationController = viewController?.navigationController,
               navigationController.viewControllers.count > 1 {
                navigationController.popViewController(animated: true)
                Orchard.v("[NavigationHandler] Popped navigation controller")
                return nil
            }
            
            if let viewController = viewController,
               viewController.presentingViewController != nil {
                await withCheckedContinuation { continuation in
                    viewController.dismiss(animated: true) {
                        Orchard.v("[NavigationHandler] Dismissed modal view controller")
                        continuation.resume()
                    }
                }
                return nil
            }
            
            Orchard.w("[NavigationHandler] No back navigation available, exiting app")
            exit(0)
        }
        
        if !urlString.isEmpty {
            guard let url = URL(string: urlString) else {
                throw BridgeError.invalidParameter("Invalid URL: \(urlString)")
            }
            
            if isExternal {
                await UIApplication.shared.open(url)
            } else {
                Orchard.v("[NavigationHandler] Internal navigation to: \(urlString)")
                TabNavigationService.shared.switchToTab(1)
            }
            return nil
        }
        
        throw BridgeError.invalidParameter("Missing navigation parameter")
    }
}

