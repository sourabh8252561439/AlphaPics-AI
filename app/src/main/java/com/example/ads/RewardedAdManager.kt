package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val REWARDED_TAG = "RewardedAdManager"

/**
 * Production rewarded-ad gate. The caller owns token persistence and the protected action.
 * A reward callback is delivered exactly once and only from AdMob's onUserEarnedReward event.
 */
object RewardedAdManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        try {
            if (isLoading || rewardedAd != null) return
            isLoading = true
            RewardedAd.load(
                context.applicationContext,
                AdConfig.REWARDED_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        isLoading = false
                        rewardedAd = ad
                        Log.d(REWARDED_TAG, "Rewarded ad loaded")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoading = false
                        rewardedAd = null
                        Log.d(REWARDED_TAG, "Rewarded ad failed to load: ${adError.message}")
                    }
                }
            )
        } catch (error: Throwable) {
            isLoading = false
            rewardedAd = null
            Log.e(REWARDED_TAG, "Rewarded preload failed", error)
        }
    }

    /**
     * Shows a loaded rewarded ad after an explicit user opt-in.
     *
     * [onRewardEarned] is invoked only from onUserEarnedReward. Dismissing the ad before that
     * callback invokes [onCancelled] and never grants access to the protected compression.
     */
    fun show(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onCancelled: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        try {
            val ad = rewardedAd
            if (ad == null) {
                preload(activity)
                onUnavailable()
                return
            }

            var rewardEarned = false
            var terminalCallbackSent = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // A RewardedAd object is single-use. Clear the shared reference immediately.
                    rewardedAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    preload(activity)
                    if (!rewardEarned && !terminalCallbackSent) {
                        terminalCallbackSent = true
                        onCancelled()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    preload(activity)
                    Log.d(REWARDED_TAG, "Rewarded ad failed to show: ${adError.message}")
                    if (!terminalCallbackSent) {
                        terminalCallbackSent = true
                        onUnavailable()
                    }
                }
            }

            ad.show(
                activity,
                object : OnUserEarnedRewardListener {
                    override fun onUserEarnedReward(rewardItem: RewardItem) {
                        if (!rewardEarned && !terminalCallbackSent) {
                            rewardEarned = true
                            terminalCallbackSent = true
                            Log.d(
                                REWARDED_TAG,
                                "Reward earned: ${rewardItem.amount} ${rewardItem.type}"
                            )
                            onRewardEarned()
                        }
                    }
                }
            )
        } catch (error: Throwable) {
            rewardedAd = null
            preload(activity)
            Log.e(REWARDED_TAG, "Rewarded show failed", error)
            onUnavailable()
        }
    }
}
