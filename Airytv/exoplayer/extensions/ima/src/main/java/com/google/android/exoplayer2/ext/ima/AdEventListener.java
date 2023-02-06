package com.google.android.exoplayer2.ext.ima;

import androidx.annotation.Nullable;

import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;

public interface AdEventListener {
  void onAdEvent(@Nullable AdEvent event, @Nullable ImaAdsLoader adsLoader);
  void onAdError(@Nullable AdErrorEvent event, @Nullable ImaAdsLoader adsLoader);
}
