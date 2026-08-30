package com.example.ads

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
class DailyTokenManagerTest {
    private lateinit var context: Context

    @Before
    fun clearState() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("daily_compression_token_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun newCycleStartsWithThreeTokens() {
        assertEquals(3, DailyTokenManager.getAvailableTokens(context, nowMillis = 1_000L))
    }

    @Test
    fun successfulCompressionConsumesExactlyOneToken() {
        val now = 10_000L
        assertTrue(DailyTokenManager.consumeAfterSuccessfulCompression(context, now))
        assertEquals(2, DailyTokenManager.getAvailableTokens(context, now))
        assertTrue(DailyTokenManager.consumeAfterSuccessfulCompression(context, now))
        assertTrue(DailyTokenManager.consumeAfterSuccessfulCompression(context, now))
        assertEquals(0, DailyTokenManager.getAvailableTokens(context, now))
        assertFalse(DailyTokenManager.consumeAfterSuccessfulCompression(context, now))
        assertEquals(0, DailyTokenManager.getAvailableTokens(context, now))
    }

    @Test
    fun earnedRewardAddsOneToken() {
        val now = 20_000L
        repeat(3) { DailyTokenManager.consumeAfterSuccessfulCompression(context, now) }
        assertEquals(0, DailyTokenManager.getAvailableTokens(context, now))
        assertEquals(1, DailyTokenManager.grantRewardToken(context, now))
        assertEquals(1, DailyTokenManager.getAvailableTokens(context, now))
    }

    @Test
    fun fullTwentyFourHourCycleResetsBalanceToThree() {
        val start = 30_000L
        repeat(3) { DailyTokenManager.consumeAfterSuccessfulCompression(context, start) }
        assertEquals(0, DailyTokenManager.getAvailableTokens(context, start))

        val after24Hours = start + 24L * 60L * 60L * 1000L
        assertEquals(3, DailyTokenManager.getAvailableTokens(context, after24Hours))
    }
}
