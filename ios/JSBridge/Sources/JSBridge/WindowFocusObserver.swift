import UIKit

/// Marker protocol for view controllers that want automatic window focus
/// change detection -- analogous to Android's `Activity.onWindowFocusChanged`.
///
/// This protocol is a pure UIKit concern. It has no dependency on
/// `JavaScriptBridge` or any bridge-specific types.
///
/// ## Adoption
///
/// 1. Call `windowFocusDidAppear()` from `viewDidAppear(_:)`.
/// 2. Call `windowFocusWillDisappear()` from `viewWillDisappear(_:)`.
/// 3. Implement `onWindowFocusChanged(hasFocus:)`.
@MainActor
public protocol WindowFocusObserver: UIViewController {

    /// Whether this view controller wants focus monitoring.
    /// Default is `true`. Return `false` to opt out.
    var wantsToListenOnFocusEvents: Bool { get }

    /// Called exactly once per state transition.
    func onWindowFocusChanged(hasFocus: Bool)
}
