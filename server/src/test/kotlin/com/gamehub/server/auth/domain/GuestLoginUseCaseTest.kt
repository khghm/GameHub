package com.gamehub.server.auth.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuestLoginUseCaseTest {

    private val useCase = GuestLoginUseCase(GuestLoginPolicy(maxGuestsPerDevice = 3))

    @Test
    fun `allows guest login when device has not reached limit`() {
        val result = useCase.execute(GuestLoginRequest("device-1", "127.0.0.1"), 2)

        assertTrue(result is GuestLoginDecision.Allowed)
    }

    @Test
    fun `rejects guest login when device has reached limit`() {
        val result = useCase.execute(GuestLoginRequest("device-1", "127.0.0.1"), 3)

        assertTrue(result is GuestLoginDecision.Rejected)
        assertEquals("تعداد مهمان‌های مجاز برای این دستگاه تمام شده است", (result as GuestLoginDecision.Rejected).message)
    }
}
