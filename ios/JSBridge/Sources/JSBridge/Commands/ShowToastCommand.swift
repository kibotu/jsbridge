import Foundation
import UIKit

/// Command for showing toast messages
public final class ShowToastCommand: BridgeCommand, @unchecked Sendable {
    public let action = "showToast"
    
    weak var viewController: UIViewController?
    
    public init(viewController: UIViewController?) {
        self.viewController = viewController
    }
    
    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let message = content?["message"] as? String else {
            throw BridgeError.invalidParameter("message")
        }
        
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        viewController?.present(alert, animated: true)
        
        let duration = (content?["duration"] as? String) == "long" ? 3.5 : 2.0
        DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
            alert.dismiss(animated: true)
        }
        
        return nil
    }
}
