package com.mergeblast.ui

import android.app.Activity
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.gms.ads.interstitial.*
import com.google.android.ump.*

class RewardedAds(private val activity: Activity) {
    var busy by mutableStateOf(false)
        private set
    var privacyRequired by mutableStateOf(false)
        private set
    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var loadedAt = 0L
    private var retryAfter = 0L
    private val interstitialId = if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        "ca-app-pub-3940256099942544/1033173712"
    else "ca-app-pub-6814851838448760/9063972308"

    fun isInterstitialReady(): Boolean = !busy && interstitial != null &&
        android.os.SystemClock.elapsedRealtime() - loadedAt < 55 * 60_000L

    fun prepareInterstitial() {
        if (busy || interstitialLoading || isInterstitialReady() || activity.isDestroyed ||
            android.os.SystemClock.elapsedRealtime() < retryAfter) return
        interstitialLoading = true
        interstitial = null
        fun load() {
            if (activity.isDestroyed) { interstitialLoading = false; return }
            InterstitialAd.load(activity, interstitialId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial = ad
                    loadedAt = android.os.SystemClock.elapsedRealtime()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    retryAfter = android.os.SystemClock.elapsedRealtime() + 60_000L
                }
            })
        }
        fun initialize() {
            privacyRequired = consent.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            if (!consent.canRequestAds()) {
                interstitialLoading = false
                retryAfter = android.os.SystemClock.elapsedRealtime() + 60_000L
                return
            }
            if (initialized) load() else MobileAds.initialize(activity) {
                activity.runOnUiThread { initialized = true; load() }
            }
        }
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initialize() }
        }, { initialize() })
    }

    fun showInterstitial(onFinished: () -> Unit) {
        val ad = interstitial
        if (!isInterstitialReady() || ad == null || activity.isDestroyed || activity.isFinishing) {
            onFinished()
            return
        }
        interstitial = null
        busy = true
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            busy = false
            onFinished()
            prepareInterstitial()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        ad.show(activity)
    }
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private val unitId = if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        "ca-app-pub-3940256099942544/5224354917"
    else "ca-app-pub-6814851838448760/4416306973"

    fun watch(onReward: () -> Unit, onUnavailable: () -> Unit) {
        if (busy || activity.isDestroyed || activity.isFinishing) return
        busy = true
        fun unavailable() {
            busy = false
            if (!activity.isDestroyed) onUnavailable()
        }
        fun load() {
            if (activity.isDestroyed || activity.isFinishing) { busy = false; return }
            RewardedAd.load(activity, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) = unavailable()
                override fun onAdLoaded(ad: RewardedAd) {
                    if (activity.isDestroyed || activity.isFinishing) { busy = false; return }
                    var rewarded = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() { busy = false }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) = unavailable()
                    }
                    ad.show(activity) {
                        if (!rewarded) {
                            rewarded = true
                            onReward()
                        }
                    }
                }
            })
        }
        fun requestAd() {
            privacyRequired = consent.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            if (!consent.canRequestAds()) { unavailable(); return }
            if (initialized) load() else {
                MobileAds.initialize(activity) {
                    activity.runOnUiThread { initialized = true; load() }
                }
            }
        }
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { requestAd() }
        }, { requestAd() })
    }

    fun showPrivacyOptions() {
        if (busy) return
        busy = true
        interstitial = null
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { busy = false }
    }
}
