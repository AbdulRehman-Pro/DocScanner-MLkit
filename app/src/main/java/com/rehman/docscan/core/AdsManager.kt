package com.rehman.docscan.core

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.rehman.docscan.R

object AdsManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private const val TAG = "AdsManager"

    fun initializeAdMob(context: Context) {
        MobileAds.initialize(context){
            Log.d(TAG, "AdMob initialized")
        }
        loadRewardedAd(context)
    }

    fun loadRewardedAd(context: Context) {
        if (isLoading || rewardedAd != null) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            context.getString(R.string.rewarded_ad_unit_id), // Replace with your Ad Unit ID
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Failed to load ad: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onUserEarnedReward: (Int) -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    rewardedAd = null // Ad reference cleared after showing
                }

                override fun onAdDismissedFullScreenContent() {
                    loadRewardedAd(activity.applicationContext) // Preload the next ad
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    Toast.makeText(
                        activity,
                        "Unable to display ad: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadRewardedAd(activity.applicationContext)
                }
            }

            ad.show(activity) { rewardItem ->
                onUserEarnedReward(Utils.AD_COINS)
            }
        } else {
            Toast.makeText(activity, "Please wait — the ad is still loading.", Toast.LENGTH_SHORT)
                .show()
            loadRewardedAd(activity.applicationContext)
        }
    }

    fun loadBannerAd(adView: AdView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}
