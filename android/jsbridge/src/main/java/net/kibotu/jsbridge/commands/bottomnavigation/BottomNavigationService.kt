package net.kibotu.jsbridge.commands.bottomnavigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow


/** Holds bottom navigation visibility so it can be controlled by bridge commands. */
object BottomNavigationService {

    val isVisible: SharedFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    fun setVisible(visible: Boolean) {
        isVisible.value = visible
    }

    fun currentVisibility(): Boolean = isVisible.value
}