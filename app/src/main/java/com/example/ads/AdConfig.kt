package com.example.ads

import com.example.BuildConfig

/**
 * Centralized AdMob ad unit configuration.
 *
 * Debug builds (e.g. anything run from Android Studio's Run button) always resolve to
 * Google's official sample test ad unit IDs, which are guaranteed to return test creatives
 * for every request. This means development and internal testing can never accidentally
 * generate invalid traffic against the real AdMob account, even without configuring a
 * specific test device ID.
 *
 * Release builds resolve to the real, production ad unit IDs.
 */
object AdConfig {

    // Real AdMob App ID - also set in AndroidManifest.xml.
    const val APPLICATION_ID = "ca-app-pub-3129438098804302~7623220553"

    // ---- Real production ad unit IDs ----
    private const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-3129438098804302/2202488438"
    private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3129438098804302/5539098213"
    private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-3129438098804302/8790720151"

    // ---- Google's official sample test ad unit IDs (always safe) ----
    // See: https://developers.google.com/admob/android/test-ads
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
}
