// shared/src/commonMain/kotlin/com/gamehub/shared/time/TimeSyncClient.kt
package com.gamehub.shared.time

class TimeSyncClient {
    fun getEstimatedServerTime(): Long = System.currentTimeMillis()
}