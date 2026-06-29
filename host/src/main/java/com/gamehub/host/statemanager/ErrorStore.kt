// host/src/main/java/com/gamehub/host/statemanager/ErrorStore.kt
package com.gamehub.host.statemanager

import com.gamehub.shared.state.Store
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UiError(
    val code: String,
    val message: String,
    val category: String = "system"
)

class ErrorStore : Store {

    private val _errors = MutableSharedFlow<UiError>(replay = 0, extraBufferCapacity = 20)
    val errors: SharedFlow<UiError> = _errors.asSharedFlow()

    override suspend fun start() {}

    override suspend fun stop() {}

    fun postError(error: UiError) {
        _errors.tryEmit(error)
    }
}