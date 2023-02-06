package com.kokoconnect.android.model.ads.interstitial;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.kokoconnect.android.AiryTvApp;
import com.kokoconnect.android.model.event.AmsEventsFacade;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

import timber.log.Timber;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

public class AppOpenManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
    private Activity currentActivity;

    private static final String LOG_TAG = "AppOpenManager";
    private AppOpenAd appOpenAd = null;

    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private static boolean isShowingAd = false;
    private final AiryTvApp myApplication;
    private long loadTime = 0;
    private String adUnitId = null;

    private AmsEventsFacade getAms() {
        return myApplication.getAms();
    }

    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        showAdIfAvailable();
        Log.d(LOG_TAG, "onStart");
    }

    /**
     * Constructor
     */
    public AppOpenManager(AiryTvApp myApplication, String adUnitId) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        this.adUnitId = adUnitId;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /**
     * Shows the ad if one isn't already showing.
     */
    public void showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable()) {
            Log.d(LOG_TAG, "Will show ad.");

            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            AppOpenManager.this.appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
//                            getAms().sendAdEventLoadFail(
//                                    INTERSTITIAL_ON_APP_LOAD,
//                                    adUnitId,
//                                    "onAdFailedToShowFullScreenContent " + adError.toString(),
//                                    AD_TYPE_ON_APP_LOAD);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
//                            getAms().sendAdEventShow(
//                                    INTERSTITIAL_ON_APP_LOAD,
//                                    adUnitId,
//                                    null,
//                                    AD_TYPE_ON_APP_LOAD);
                            isShowingAd = true;
                        }
                    };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
        } else {
            Log.d(LOG_TAG, "Can not show ad.");
            fetchAd();
        }
    }

    public void release() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        appOpenAd = null;
        isShowingAd = false;
    }

    /**
     * Request an ad
     */
    public void fetchAd() {
        // Have unused ad, no need to fetch another.
        if (isAdAvailable()) {
            return;
        }

        loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                AppOpenManager.this.appOpenAd = appOpenAd;
                AppOpenManager.this.loadTime = (new Date()).getTime();
//                getAms().sendAdEventLoaded(
//                        INTERSTITIAL_ON_APP_LOAD,
//                        adUnitId,
//                        null,
//                        AD_TYPE_ON_APP_LOAD
//                );
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                getAms().sendAdEventLoadFail(
//                        INTERSTITIAL_ON_APP_LOAD,
//                        adUnitId,
//                        "onAppOpenAdFailedToLoad error code: " + loadAdError,
//                        AD_TYPE_ON_APP_LOAD);
            }
        };
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                myApplication, adUnitId, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }

    /**
     * Creates and returns ad request.
     */
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    /**
     * ActivityLifecycleCallback methods
     */

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
    }

    public void setAdUnitId(String adUnitId) {
        if (adUnitId != null && !adUnitId.isEmpty()) {
            if (!adUnitId.equals(this.adUnitId)) {
                this.adUnitId = adUnitId;
                appOpenAd = null;
                isShowingAd = false;
                fetchAd();
                Timber.d("AppOpenManager: changed unitId -> %s", adUnitId);
            }
        }
    }

}