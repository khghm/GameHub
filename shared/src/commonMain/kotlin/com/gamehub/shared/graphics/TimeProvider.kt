package com.gamehub.shared.graphics

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun currentTimeNanos(): Long
}

class DefaultTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun currentTimeNanos(): Long = System.nanoTime()
}

class TestTimeProvider(
    private var currentTimeMs: Long = 0L
) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTimeMs
    override fun currentTimeNanos(): Long = currentTimeMs * 1_000_000L

    fun advanceTimeByMs(delta: Long) {
        currentTimeMs += delta
    }
}
