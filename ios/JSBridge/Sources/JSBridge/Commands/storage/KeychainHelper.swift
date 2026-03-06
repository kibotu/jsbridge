import Foundation
import Security

/// Helper class for Keychain operations
///
/// **Why a separate helper class?**
/// - Encapsulates Keychain's complex Security framework APIs
/// - Shared by multiple bridge commands (save/load/remove secure data)
/// - Makes testing easier by centralizing Keychain logic
/// - Provides a clean, simple API over Apple's C-style Security APIs
///
/// **Security Configuration:**
/// Uses `kSecAttrAccessibleWhenUnlocked` which means:
/// - Data is only accessible when device is unlocked
/// - Balances security with usability
/// - Prevents access to sensitive data if device is stolen and locked
///
/// **Design Decision:**
/// Stores data as generic password items (kSecClassGenericPassword) because:
/// - It's the most flexible Keychain item type
/// - Works for any key-value pair
/// - Doesn't require certificates or special configurations
public class KeychainHelper {
    static func save(key: String, value: String) -> Bool {
        guard let data = value.data(using: .utf8) else { return false }
        
        // Delete any existing item first
        _ = delete(key: key)
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlocked
        ]
        
        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }
    
    static func load(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data,
              let string = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        return string
    }
    
    static func delete(key: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}

