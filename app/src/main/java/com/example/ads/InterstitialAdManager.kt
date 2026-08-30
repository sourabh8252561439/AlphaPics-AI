package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val TAG = "InterstitialAdManager"

/**
 * Loads and shows the interstitial ad shown after a successful image compression.
 *
 * Usage:
 * - Call [preload] once, early (e.g. from the Application class after Mobile Ads SDK init),
 *   to have an ad ready by the time the user finishes their first compression.
 * - Call [onSuccessfulCompression] once per successful compression + save. It only actually
 *   shows an ad every [SHOW_EVERY_NTH_SUCCESS]th call; the rest just keep a preload warm.
 *   If no ad has finished loading on a turn where one should show, it does nothing that turn
 *   (never blocks or delays the user) and kicks off another load attempt for next time.
 * - After an ad is shown and dismissed (or fails to show), the next one is preloaded
 *   automatically.
 */
object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    // Show the interstitial only every Nth successful compression, not every single one.
    private const val SHOW_EVERY_NTH_SUCCESS = 3
    private var successCount = 0

    fun preload(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    Log.d(TAG, "Interstitial ad failed to load: ${adError.message}")
                }
            }
        )
    }

    /**
     * Call exactly once per successful compression + save. Counts these calls internally and
     * only shows the interstitial on every [SHOW_EVERY_NTH_SUCCESS]th one (3rd, 6th, 9th...);
     * every other call just makes sure a preload is in flight and returns.
     *
     * Always safe to call unconditionally from the compression success path: if no ad happens
     * to be loaded on a turn where one should show, this quietly does nothing further (beyond
     * kicking off another preload) and never blocks, delays, or interferes with the
     * compression/save flow that already completed.
     */
    fun onSuccessfulCompression(activity: Activity) {
        successCount++

        // Always keep one loading/loaded regardless of whether this turn shows it - this is a
        // no-op if a load is already in flight or an ad is already sitting ready.
        preload(activity)

        if (successCount % SHOW_EVERY_NTH_SUCCESS != 0) return

        val ad = interstitialAd ?: return // Not ready this turn - flow continues normally.

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preload(activity)
            }
        }
        ad.show(activity)
    }
}
