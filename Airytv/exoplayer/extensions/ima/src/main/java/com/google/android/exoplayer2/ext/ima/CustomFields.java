package com.google.android.exoplayer2.ext.ima;

import androidx.annotation.Nullable;

import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;

public class CustomFields implements AdEventListener {

  @Nullable private ImaBridge imaData;
  @Nullable private AdEventListener listener;

  public CustomFields(ImaBridge imaData, AdEventListener listener) {
    this.imaData = imaData;
    this.listener = listener;
  }

  @Override
  public void onAdEvent(@Nullable AdEvent event, @Nullable ImaAdsLoader adsLoader) {
    if (listener != null) {
      listener.onAdEvent(event, adsLoader);
    }
  }

  @Override
  public void onAdError(@Nullable AdErrorEvent event, @Nullable ImaAdsLoader adsLoader) {
    if (listener != null) {
      listener.onAdError(event, adsLoader);
    }
  }

  @Nullable
  public ImaBridge getImaData() {
    return imaData;
  }

  public interface ImaBridge {
    public String getName();
    public String getVastXml();
  }

}
