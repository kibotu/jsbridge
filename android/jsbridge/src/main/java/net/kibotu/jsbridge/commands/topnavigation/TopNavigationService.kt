package net.kibotu.jsbridge.commands.topnavigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple holder for the current top navigation configuration so it can be
 * updated by bridge commands and observed by UI (e.g., MainActivity).
 */
object TopNavigationService {

    val config: SharedFlow<TopNavigationConfig>
        field = MutableStateFlow<TopNavigationConfig>(TopNavigationConfig())

    fun applyConfig(newConfig: TopNavigationConfig) {
        config.value = newConfig
    }

    fun currentConfig(): TopNavigationConfig = config.value
}