package net.kibotu.jsbridge.commands.refresh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

object RefreshService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val onRefresh: SharedFlow<String?>
        field = MutableSharedFlow<String?>()

    fun refresh(url: String?) {
        scope.launch(Dispatchers.Main) {
            onRefresh.emit(url)
        }
    }
}
