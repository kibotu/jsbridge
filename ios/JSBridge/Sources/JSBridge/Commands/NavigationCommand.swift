import Foundation
import UIKit
import WebKit
import Orchard

/// Handles multiple navigation patterns: back navigation, internal/external URLs.
public final class NavigationCommand: BridgeCommand, @unchecked Sendable {
    public let action = "navigation"
    
    weak var viewController: UIViewController?
    weak var webView: WKWebView?
    
    public init(viewController: UIViewController?, webView: WKWebView? = nil) {
        self.viewController = viewController
        self.webView = webView
    }
    
    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        let urlString = content?["url"] as? String ?? ""
        let isExternal = content?["external"] as? Bool ?? false
        let goBack = content?["goBack"] as? Bool ?? false
        
        Orchard.v("[NavigationCommand] url=\(urlString) external=\(isExternal) goBack=\(goBack)")
        
        if goBack {
            if let webView = webView, webView.canGoBack {
                webView.goBack()
                Orchard.v("[NavigationCommand] Navigated back in WebView history")
                return nil
            }
            
            if let navigationController = viewController?.navigationController,
               navigationController.viewControllers.count > 1 {
                navigationController.popViewController(animated: true)
                Orchard.v("[NavigationCommand] Popped navigation controller")
                return nil
            }
            
            if let viewController = viewController,
               viewController.presentingViewController != nil {
                await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
                    viewController.dismiss(animated: true) {
                        Orchard.v("[NavigationCommand] Dismissed modal view controller")
                        continuation.resume()
                    }
                }
                return nil
            }
            
            Orchard.w("[NavigationCommand] No back navigation available, exiting app")
            exit(0)
        }
        
        if !urlString.isEmpty {
            guard let url = URL(string: urlString) else {
                throw BridgeError.invalidParameter("Invalid URL: \(urlString)")
            }
            
            if isExternal {
                await UIApplication.shared.open(url)
            } else {
                Orchard.v("[NavigationCommand] Internal navigation to: \(urlString)")
                TabNavigationService.shared.switchToTab(1)
            }
            return nil
        }
        
        throw BridgeError.invalidParameter("Missing navigation parameter")
    }
}
