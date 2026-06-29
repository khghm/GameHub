// shared/src/commonMain/kotlin/com/gamehub/shared/state/Store.kt
package com.gamehub.shared.state

import kotlinx.coroutines.flow.StateFlow

interface Store {
    suspend fun start()
    suspend fun stop()
}