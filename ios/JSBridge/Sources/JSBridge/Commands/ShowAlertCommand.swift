import Foundation
import UIKit

/// Command for showing alert dialogs
///
/// **Why allow web to show alerts?**
/// - Native alerts look and feel better than web-based modals
/// - Provides consistent UX across the app
/// - Enables confirmation dialogs, warnings, and important messages
/// - Users trust native alerts more than web popups
///
/// **Design Decision:**
/// Supports custom buttons via the `buttons` parameter. If not provided,
/// defaults to a single "OK" button. This keeps the simple case simple
/// while allowing more complex alert dialogs when needed.
///
/// **Limitation:**
/// Current implementation doesn't report which button was clicked back to JavaScript.
/// This could be enhanced if needed by adding a callback mechanism.
public class ShowAlertCommand: BridgeCommand {
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

