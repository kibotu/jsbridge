package net.kibotu.jsbridge.commands

/**
 * Contract for implementing a single bridge command handler (Strategy Pattern).
 *
 * **Why this interface:**
 * Enables adding new bridge capabilities without modifying existing code (Open-Closed Principle).
 * Each command is self-contained: its own parsing, validation, execution, and error handling.
 *
 * **Why one action per command:**
 * Enforces Single Responsibility - each command does one thing well. Makes commands:
 * - Easy to test in isolation
 * - Easy to understand (no complex routing within command)
 * - Easy to maintain (changes to one action don't affect others)
 * - Easy to reuse across different bridge implementations
 *
 * **Why suspend:**
 * Commands often perform async operations (network, storage, UI). Suspend functions
 * allow efficient non-blocking execution without callback hell.
 *
 * **Design philosophy:**
 * Commands should be stateless, reusable, and independently testable. All state
 * comes from `content` parameter or injected dependencies.
 */
interface BridgeCommand {

    /**
     * The action identifier this command handles (e.g., "deviceInfo", "showToast").
     *
     * **Why string identifier:**
     * Matches web API contract where JavaScript sends action names as strings.
     * Must be unique across all registered commands.
     */
    val action: String

    /**
     * Executes the bridge command with provided parameters.
     *
     * **Why nullable return:**
     * - Non-null: Request-response commands that return data to web
     * - Null: Fire-and-forget commands (tracking, haptic) that don't need responses
     *
     * **Why nullable content:**
     * Some commands need no parameters (deviceInfo), others require complex objects.
     * Implementation is responsible for parsing and validating content.
     *
     * **Error handling:**
     * Commands should catch their own errors and return error response objects.
     * Throwing exceptions should be rare (only for unexpected/unrecoverable errors).
     *
     * @param content Optional parameters from web, typically JSONObject or JSON-serializable data
     * @return Response object for web (success/error) or null for fire-and-forget
     */
    suspend fun handle(content: Any?): Any?
}

