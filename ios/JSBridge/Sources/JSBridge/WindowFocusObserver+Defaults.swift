import UIKit

private enum AssociatedKeys {
    nonisolated(unsafe) static var currentlyHasFocus: UInt8 = 0
    nonisolated(unsafe) static var focusCheckTimer: UInt8 = 0
    nonisolated(unsafe) static var backgroundObserver: UInt8 = 0
    nonisolated(unsafe) static var foregroundObserver: UInt8 = 0
}

// MARK: - Default property

extension WindowFocusObserver {

    public var wantsToListenOnFocusEvents: Bool { true }
}

// MARK: - Hook methods

extension WindowFocusObserver {

    /// Call from `viewDidAppear(_:)`.
    public func windowFocusDidAppear() {
        let hasFocus = isCurrentlyFocused()
        dispatchFocusChange(hasFocus)
        if wantsToListenOnFocusEvents {
            startFocusMonitoring()
        }
    }

    /// Call from `viewWillDisappear(_:)`.
    public func windowFocusWillDisappear() {
        dispatchFocusChange(false)
        if wantsToListenOnFocusEvents {
            stopFocusMonitoring()
        }
    }

    /// Tears down timer and notification observers.
    public func cleanupFocusObserver() {
        stopFocusMonitoring()
    }
}

// MARK: - Associated-object state

extension WindowFocusObserver {

    private var currentlyHasFocus: Bool? {
        get { objc_getAssociatedObject(self, &AssociatedKeys.currentlyHasFocus) as? Bool }
        set { objc_setAssociatedObject(self, &AssociatedKeys.currentlyHasFocus, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    private var focusCheckTimer: Timer? {
        get { objc_getAssociatedObject(self, &AssociatedKeys.focusCheckTimer) as? Timer }
        set { objc_setAssociatedObject(self, &AssociatedKeys.focusCheckTimer, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    private var backgroundObserver: NSObjectProtocol? {
        get { objc_getAssociatedObject(self, &AssociatedKeys.backgroundObserver) as? NSObjectProtocol }
        set { objc_setAssociatedObject(self, &AssociatedKeys.backgroundObserver, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    private var foregroundObserver: NSObjectProtocol? {
        get { objc_getAssociatedObject(self, &AssociatedKeys.foregroundObserver) as? NSObjectProtocol }
        set { objc_setAssociatedObject(self, &AssociatedKeys.foregroundObserver, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
}

// MARK: - Focus state management

extension WindowFocusObserver {

    private func dispatchFocusChange(_ hasFocus: Bool) {
        guard currentlyHasFocus != hasFocus else { return }
        currentlyHasFocus = hasFocus
        onWindowFocusChanged(hasFocus: hasFocus)
    }
}

// MARK: - Focus detection

extension WindowFocusObserver {

    private func isCurrentlyFocused() -> Bool {
        guard UIApplication.shared.applicationState == .active else { return false }
        guard isViewLoaded, view.window != nil else { return false }

        if let keyWindow = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow),
           keyWindow != view.window {
            return false
        }

        if presentedViewController != nil { return false }
        if navigationController?.presentedViewController != nil { return false }
        if tabBarController?.presentedViewController != nil { return false }

        return true
    }
}

// MARK: - Monitoring

extension WindowFocusObserver {

    private func startFocusMonitoring() {
        guard wantsToListenOnFocusEvents else { return }
        addAppLifecycleObservers()

        focusCheckTimer?.invalidate()
        focusCheckTimer = Timer.scheduledTimer(withTimeInterval: 1.0 / 60.0, repeats: true) { [weak self] _ in
            guard let self else { return }
            self.dispatchFocusChange(self.isCurrentlyFocused())
        }
    }

    private func stopFocusMonitoring() {
        focusCheckTimer?.invalidate()
        focusCheckTimer = nil
        removeAppLifecycleObservers()
    }
}

// MARK: - App lifecycle notifications

extension WindowFocusObserver {

    private func addAppLifecycleObservers() {
        guard backgroundObserver == nil else { return }

        let nc = NotificationCenter.default

        backgroundObserver = nc.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: nil, queue: .main
        ) { [weak self] _ in
            guard let self else { return }
            MainActor.assumeIsolated {
                self.dispatchFocusChange(false)
            }
        }

        foregroundObserver = nc.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil, queue: .main
        ) { [weak self] _ in
            guard let self else { return }
            MainActor.assumeIsolated {
                guard self.isViewLoaded, self.view.window != nil else { return }
                self.dispatchFocusChange(self.isCurrentlyFocused())
            }
        }
    }

    private func removeAppLifecycleObservers() {
        let nc = NotificationCenter.default
        if let observer = backgroundObserver {
            nc.removeObserver(observer)
            backgroundObserver = nil
        }
        if let observer = foregroundObserver {
            nc.removeObserver(observer)
            foregroundObserver = nil
        }
    }
}
