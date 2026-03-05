import Foundation

/// Handler for refresh command
///
/// **Why allow web to trigger refresh?**
/// - Enables pull-to-refresh functionality from web content
/// - Allows web to refresh native app state (user profile, settings, etc.)
/// - Supports synchronization between web and native data
/// - Provides consistent refresh behavior across the app
///
/// **Design Decision:**
/// Uses a centralized RefreshService rather than directly refreshing the WebView.
/// This allows:
/// - Native components to react to refresh requests
/// - Coordinated refresh across multiple data sources
/// - Consistent refresh behavior app-wide
///
/// **Note:**
/// This handler doesn't call completion. This appears to be intentional for
/// fire-and-forget behavior, where the web doesn't need confirmation that
/// refresh started. Consider whether completion should be called based on
/// your app's requirements.
public class RefreshHandler: BridgeCommand {
    public let actionName = "refresh"

    public init() {}

//    @CoreInject var refreshService: RefreshService
    
    public func handle(content: [String: Any]?) async throws -> [String: Any]? {
//        let appLink = PluginController.createAppLink(from:  "https://experts.net.kibotu/refresh".url!, source: .pushNotification(isSilent: true))
//        refreshService.onRefresh(appLink: appLink)
        return nil
    }
}
