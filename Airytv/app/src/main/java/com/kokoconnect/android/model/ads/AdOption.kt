package com.kokoconnect.android.model.ads

import com.google.gson.annotations.SerializedName

abstract class AdOption {
    var enable: Boolean = false
}

class AdsOptions {
    @SerializedName("enable")
    var adsEnabled: Boolean = false
    var interstitial = InterstitialOptions()
    var vast = VastOptions()
    var infomercial = InfomercialOptions()
    @SerializedName("channels")
    var cueTonesChannels: List<Int> = emptyList()
    @SerializedName("channelsNoInterstitials")
    var noInterstitialChannels: List<Int> = emptyList()
}

class FirstTune : AdOption() {
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class CueTones : AdOption() {
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class ProgramChange : AdOption() {
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class ChannelChange : AdOption() {
    @SerializedName("number_channels")
    var numberChannels: Int = 0
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class ScreenChange : AdOption() {
    @SerializedName("number_screens")
    var numberScreens: Int = 0
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class Timer : AdOption() {
    @SerializedName("interval")
    var timerInterval: Long = 0
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class StartTimer : AdOption() {
    @SerializedName("interval")
    var timerInterval: Long = 0
    @SerializedName("number_ads")
    var numberAds: Int = 0
}

class InterstitialOptions {
    var start = StartTimer()
    var timer = Timer()
    @SerializedName("channel_change")
    var channelChange = ChannelChange()
    @SerializedName("screen_change")
    var screenChange = ScreenChange()
}

class VastOptions {
    @SerializedName("first_tune")
    var firstTune = FirstTune()
    @SerializedName("cue_tones")
    var cueTones = CueTones()
    @SerializedName("program_change")
    var programChange = ProgramChange()
    @SerializedName("channel_change")
    var channelChange = ChannelChange()
    var timer = Timer()
    @SerializedName("timer_vod")
    var vodTimer = Timer()
}

class InfomercialOptions {
    @SerializedName("first_tune")
    var firstTune = FirstTune()
    @SerializedName("cue_tones")
    var cueTones = CueTones()
    @SerializedName("program_change")
    var programChange = ProgramChange()
    @SerializedName("channel_change")
    var channelChange = ChannelChange()
    var timer = Timer()
}

