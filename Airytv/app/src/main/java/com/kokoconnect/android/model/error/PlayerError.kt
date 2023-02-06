package com.kokoconnect.android.model.error

import android.content.Context
import android.text.format.DateUtils
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.model.vod.Content


open class PlayerError(
    var type: PlayerErrorType = PlayerErrorType.NONE,
    var exception: Exception? = null
) {

}

class EmptyPlayerError : PlayerError()

class YouTubePlayerError(
    val reason: String,
    val whoLastLoadVideo: String,
    val cueId: String,
    val currentPositionSec: Long
) : PlayerError(
    PlayerErrorType.YOUTUBE_PLAYER_ERROR
) {

    fun prepareException(
        context: Context?,
        programDescription: ProgramDescription?
    ) {
        val amsId = context?.let {
            Preferences(it).Ams().getAmsId()
        } ?: ""
        val channelNumber = programDescription?.channelNumber ?: ""
        val channelName = programDescription?.channelName ?: ""
        val programName = programDescription?.programName ?: ""
        val startTime = DateUtils.formatElapsedTime(programDescription?.programStartSecs ?: 0L)
        val setPosition = DateUtils.formatElapsedTime(currentPositionSec)
        val duration = DateUtils.formatElapsedTime(programDescription?.realProgramDuration?.toLong() ?: 0L)
        val info = "${reason} - " +
                "amsId: ${amsId}\n" +
                "url: $cueId\n" +
                "whoLastLoadVideo: $whoLastLoadVideo\n" +
                "channel: ${channelNumber}\n" +
                "channelName : ${channelName}\n" +
                "program: ${programName}\n" +
                "startTime: ${startTime}\n" +
                "setPostition: $setPosition\n" +
                "duration: $duration"
        exception = Exception(info)
    }

    fun prepareException(
        context: Context?,
        content: Content?
    ) {
        val amsId = context?.let {
            Preferences(it).Ams().getAmsId()
        } ?: ""
        val type = content?.type ?: ""
        val mediaType = content?.mediaType ?: ""
        val name = content?.name ?: ""
        val id = content?.id?.toString() ?: ""
        val info = "${reason} - " +
                "amsId: ${amsId}\n" +
                "url: $cueId\n" +
                "whoLastLoadVideo: $whoLastLoadVideo\n" +
                "type: ${type}\n" +
                "mediaType : ${mediaType}\n" +
                "name: ${name}\n" +
                "id: ${id}"
        exception = Exception(info)
    }
}


enum class PlayerErrorType {
    NONE,
    NO_INTERNET_CONNECTION,
    PRIVATE_ARCHIVE_CONTENT,
    YOUTUBE_PLAYER_UNAVAILABLE,
    YOUTUBE_PLAYER_ERROR,
    EXOPLAYER_PLAYER_UNAVAILABLE,
    EXOPLAYER_PLAYER_STATE_IDLE,
    DAILYMOTION_PLAYER_UNAVAILABLE
}