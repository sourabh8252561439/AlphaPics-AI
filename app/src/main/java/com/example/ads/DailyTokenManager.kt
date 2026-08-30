package com.example.ads

import android.content.Context
import android.util.Log

private const val TOKEN_TAG = "DailyTokenManager"

/**
 * Device-local deterministic accounting for the free compression allowance.
 *
 * A cycle begins when the state is first initialized (or after the prior 24-hour cycle expires).
 * Every successful saved image consumes exactly one token. Rewarded ads can grant one extra token.
 */
object DailyTokenManager {
    const val INITIAL_DAILY_TOKENS = 3

    // The requested persistent state name is intentionally kept verbatim.
    private const val PREFS_NAME = "daily_compression_token_state"
    private const val TOKEN_KEY = "userDailyTokens"
    private const val LAST_RESET_AT_KEY = "userDailyTokensLastResetAt"
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    private val lock = Any()

    fun getAvailableTokens(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Int = synchronized(lock) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ensureCurrentCycle(prefs = prefs, nowMillis = nowMillis)
            prefs.getInt(TOKEN_KEY, INITIAL_DAILY_TOKENS).coerceAtLeast(0)
        } catch (error: Throwable) {
            Log.e(TOKEN_TAG, "Unable to read daily token state", error)
            0
        }
    }

    fun hasAvailableToken(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = getAvailableTokens(context, nowMillis) > 0

    /**
     * Consume one token only after a compression has successfully produced and saved an output.
     */
    fun consumeAfterSuccessfulCompression(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = synchronized(lock) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ensureCurrentCycle(prefs = prefs, nowMillis = nowMillis)
            val current = prefs.getInt(TOKEN_KEY, INITIAL_DAILY_TOKENS).coerceAtLeast(0)
            if (current <= 0) return@synchronized false

            prefs.edit().putInt(TOKEN_KEY, current - 1).commit()
        } catch (error: Throwable) {
            Log.e(TOKEN_TAG, "Unable to consume daily token", error)
            false
        }
    }

    /**
     * Grant exactly one compression token after AdMob invokes onUserEarnedReward.
     */
    fun grantRewardToken(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Int = synchronized(lock) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ensureCurrentCycle(prefs = prefs, nowMillis = nowMillis)
            val current = prefs.getInt(TOKEN_KEY, INITIAL_DAILY_TOKENS).coerceAtLeast(0)
            val updated = if (current == Int.MAX_VALUE) Int.MAX_VALUE else current + 1
            if (!prefs.edit().putInt(TOKEN_KEY, updated).commit()) {
                Log.w(TOKEN_TAG, "Reward token write did not commit")
            }
            updated
        } catch (error: Throwable) {
            Log.e(TOKEN_TAG, "Unable to grant rewarded token", error)
            0
        }
    }

    private fun ensureCurrentCycle(
        prefs: android.content.SharedPreferences,
        nowMillis: Long
    ) {
        val lastResetAt = prefs.getLong(LAST_RESET_AT_KEY, -1L)
        val hasTokenState = prefs.contains(TOKEN_KEY)

        if (!hasTokenState || lastResetAt < 0L) {
            prefs.edit()
                .putInt(TOKEN_KEY, INITIAL_DAILY_TOKENS)
                .putLong(LAST_RESET_AT_KEY, nowMillis)
                .commit()
            return
        }

        val elapsed = nowMillis - lastResetAt
        if (elapsed >= DAY_MILLIS) {
            prefs.edit()
                .putInt(TOKEN_KEY, INITIAL_DAILY_TOKENS)
                .putLong(LAST_RESET_AT_KEY, nowMillis)
                .commit()
        } else if (elapsed < 0L) {
            // Device clock moved backwards. Preserve the balance, but restart the local 24-hour
            // reference point rather than allowing a negative elapsed duration indefinitely.
            prefs.edit().putLong(LAST_RESET_AT_KEY, nowMillis).commit()
        }
    }
}
