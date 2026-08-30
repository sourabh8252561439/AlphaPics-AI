package com.example.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

/**
 * An anchored adaptive AdMob banner ad, sized to the full width of the device.
 *
 * Designed to sit in a Scaffold's `bottomBar` slot: it fills the available width and reports
 * its own height, so Scaffold automatically pads the scrollable content above it and nothing
 * gets hidden behind it.
 *
 * The ad view is created once per composition and destroyed via [DisposableEffect] when this
 * leaves the composition, so it does not leak.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val adView = remember {
        AdView(context).apply {
            adUnitId = AdConfig.BANNER_AD_UNIT_ID

            // getLargeAnchoredAdaptiveBannerAdSize is Google's current recommended API for an
            // anchored adaptive banner; the older getCurrentOrientationAnchoredAdaptiveBannerAdSize
            // is deprecated as of SDK 25.x.
            val displayMetrics = context.resources.displayMetrics
            val adWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidthDp))

            adListener = object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Non-fatal: simply means no banner shows for this request. Logged only.
                    Log.d(TAG, "Banner ad failed to load: ${adError.message}")
                }
            }
        }
    }

    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView }
    )
}
