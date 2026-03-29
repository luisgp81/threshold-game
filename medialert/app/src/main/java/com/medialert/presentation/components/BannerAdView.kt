package com.medialert.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.medialert.BuildConfig

/**
 * AdMob banner — included but controlled by BuildConfig.ADS_ENABLED.
 * Set ADS_ENABLED = true in build.gradle.kts release config to activate.
 */
@Composable
fun BannerAdView(
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Test banner ID
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.ADS_ENABLED) return

    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
