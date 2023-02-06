package com.kokoconnect.android.model.player

enum class VideoOpeningReason {
    ON_RESUME,
    ON_RECREATE_ACTIVITY,
    ON_NEED_SHOW_VIDEO_AD,
    ON_AFTER_VIDEO_AD,
    ON_CHROMECAST_CONNECTED,
    ON_CHROMECAST_DISCONNECTED,
    // TV events
    ON_TV_FIRST_TUNE,
    ON_TV_CHANNEL_CHANGE,
    ON_TV_PROGRAM_CHANGE,
    ON_TV_PROGRAM_PART_CHANGE,
    // VOD events
    ON_VOD_CONTENT_SWITCH
}