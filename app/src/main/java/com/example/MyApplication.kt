package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.ads.DailyTokenManager
import com.example.ads.InterstitialAdManager
import com.example.ads.RewardedAdManager
import com.google.android.gms.ads.MobileAds
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Refresh/initialize the local 24-hour token cycle at app launch. Every protected
        // compression path also re-checks this state before doing any work.
        DailyTokenManager.getAvailableTokens(this)

        // Initialize the Google Mobile Ads SDK on a background thread, ideally at app launch
        // and before any ad is requested. Once init completes we preload the interstitial on
        // the main thread (ad-loading calls must happen on the main thread) so it's ready by
        // the time the user finishes their first compression.
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MyApplication) {}
            withContext(Dispatchers.Main) {
                InterstitialAdManager.preload(this@MyApplication)
                RewardedAdManager.preload(this@MyApplication)
            }
        }

        // Set up the global UncaughtExceptionHandler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log the uncaught exception silently
                Log.e("MyApplication", "Uncaught exception on thread: ${thread.name}", throwable)

                // Perform a clean restart of the MainActivity dashboard
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MyApplication", "Failed to restart MainActivity on crash", e)
            } finally {
                // Terminate the crashed process cleanly to prevent any standard crash dialogues or hangs
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }
}
