package com.kokoconnect.android.model.ads.video

sealed class VideoAdTrigger(val reasonName: String, val type: Type) {
    object OnTvFirstTune : VideoAdTrigger("OnFirstTune", Type.OnFirstTune)
    class OnTvCueTones(val duration: Int) : VideoAdTrigger("OnCueTones", Type.OnCueTones)
    object OnTvProgramChange : VideoAdTrigger("OnProgramChange", Type.OnProgramChange)
    object OnTvProgramTimestamp : VideoAdTrigger("OnProgramTimestamp", Type.OnProgramTimestamp)
    object OnTvChannelChange : VideoAdTrigger("OnChannelChange", Type.OnChannelChange)
    object OnTvTimer : VideoAdTrigger("OnTimer", Type.OnTimer)

    object OnVodTimer : VideoAdTrigger("OnTimer", Type.OnTimer)

    enum class Type() {
        OnFirstTune,
        OnCueTones,
        OnProgramChange,
        OnChannelChange,
        OnTimer,
        OnProgramTimestamp
    }
}