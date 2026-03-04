import SwiftUI

/// Configuration for the top navigation bar
struct TopNavigationConfig {
    var isVisible: Bool = true
    var title: String? = "Bridge Demo"
    var showBackButton: Bool = false
    var showDivider: Bool = true
    var showLogo: Bool = false
    var showProfileIconWidget: Bool = false
}

/// Observable service for top navigation state
class TopNavigationService: ObservableObject {
    @Published var config = TopNavigationConfig()
    
    static let shared = TopNavigationService()
    
    private init() {}
    
    func configure(with config: TopNavigationConfig) {
        DispatchQueue.main.async {
            self.config = config
        }
    }
    
    func update(isVisible: Bool? = nil,
                title: String? = nil,
                showBackButton: Bool? = nil,
                showDivider: Bool? = nil,
                showLogo: Bool? = nil,
                showProfileIconWidget: Bool? = nil) {
        DispatchQueue.main.async {
            withAnimation(.easeInOut(duration: 0.3)) {
                if let isVisible = isVisible {
                    self.config.isVisible = isVisible
                }
                if let title = title {
                    self.config.title = title
                }
                if let showBackButton = showBackButton {
                    self.config.showBackButton = showBackButton
                }
                if let showDivider = showDivider {
                    self.config.showDivider = showDivider
                }
                if let showLogo = showLogo {
                    self.config.showLogo = showLogo
                }
                if let showProfileIconWidget = showProfileIconWidget {
                    self.config.showProfileIconWidget = showProfileIconWidget
                }
            }
        }
    }
}

/// Top navigation bar view
struct TopNavigationView: View {
    @ObservedObject var service = TopNavigationService.shared
    @EnvironmentObject var themeManager: ThemeManager
    let onBackPressed: () -> Void
    
    private var topSafeAreaInset: CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.top ?? 0
    }
    
    var body: some View {
        if service.config.isVisible {
            VStack(spacing: 0) {
                HStack(spacing: 12) {
                    if service.config.showBackButton {
                        Button(action: onBackPressed) {
                            Image(systemName: "chevron.left")
                                .foregroundColor(.primary)
                                .font(.system(size: 20, weight: .medium))
                        }
                    }
                    
                    if service.config.showLogo {
                        Image(systemName: "app.fill")
                            .foregroundColor(.accentBlue)
                            .font(.system(size: 24))
                    } else if let title = service.config.title {
                        Text(title)
                            .font(.headline)
                            .fontWeight(.semibold)
                    }
                    
                    Spacer()
                    
                    Button(action: { themeManager.toggle() }) {
                        Image(systemName: themeManager.isDarkMode ? "sun.max.fill" : "moon.fill")
                            .foregroundColor(.accentBlue)
                            .font(.system(size: 20))
                    }
                    
                    if service.config.showProfileIconWidget {
                        Button(action: {}) {
                            Image(systemName: "person.circle.fill")
                                .foregroundColor(.secondary)
                                .font(.system(size: 28))
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .padding(.top, topSafeAreaInset)
                .background(Color.surfaceColor)
                
                if service.config.showDivider {
                    Divider()
                }
            }
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
}

extension Color {
    init(light: Color, dark: Color) {
        self.init(UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
    }
    
    static let accentBlue = Color(
        light: Color(red: 5/255, green: 99/255, blue: 193/255),
        dark: Color(red: 5/255, green: 99/255, blue: 193/255)
    )
    
    static let surfaceColor = Color(
        light: .white,
        dark: Color(red: 30/255, green: 41/255, blue: 59/255)
    )
    
    static let slateBackground = Color(
        light: Color(red: 248/255, green: 250/255, blue: 252/255),
        dark: Color(red: 15/255, green: 23/255, blue: 42/255)
    )
    
    static let onSurfaceVariant = Color(
        light: Color(red: 71/255, green: 85/255, blue: 105/255),
        dark: Color(red: 203/255, green: 213/255, blue: 225/255)
    )
}

