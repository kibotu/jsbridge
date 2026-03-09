import Foundation
import UIKit

/// Command for showing alert dialogs
public final class ShowAlertCommand: BridgeCommand, @unchecked Sendable {
    public let action = "showAlert"
    
    weak var viewController: UIViewController?
    
    public init(viewController: UIViewController?) {
        self.viewController = viewController
    }
    
    @MainActor
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
        guard let title = content?["title"] as? String,
              let message = content?["message"] as? String else {
            throw BridgeError.invalidParameter("title or message")
        }
        
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        
        if let buttons = content?["buttons"] as? [String] {
            for buttonTitle in buttons {
                alert.addAction(UIAlertAction(title: buttonTitle, style: .default))
            }
        } else {
            alert.addAction(UIAlertAction(title: "OK", style: .default))
        }
        
        viewController?.present(alert, animated: true)
        return nil
    }
}
