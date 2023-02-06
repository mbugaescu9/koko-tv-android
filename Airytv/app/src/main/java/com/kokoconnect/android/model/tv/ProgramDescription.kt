package com.kokoconnect.android.model.tv

import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.util.*
import com.kokoconnect.android.util.DateUtils.parseIsoDate
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder

class ProgramDescription {
    var category: String = ""
        private set
    var channelNumber: Int = 0
        private set
    var channelId: Int = 0
        private set
    var channelName: String = ""
        private set
    var isChannelPrivate: Boolean = false
        private set
    var programName: String = ""
        private set
    var programDescription: String? = ""
        private set
    var programDuration: String = ""
        private set
    var realProgramDuration: Int = 0
        private set
    var programDurationSecs: Int = 0
        private set
    var programStartSecs: Long = 0
        private set
    var video: PlayerObject? = null
        private set
    var videoOpeningReason: VideoOpeningReason? = null
    var lastWatchDuration: Int = 0

    private constructor()

    constructor(video: PlayerObject?, program: Program, channel: Channel) {
        category = channel.category ?: ""
        channelNumber = channel.number
        channelId = channel.id
        channelName = channel.name
        isChannelPrivate = channel.private
        programName = program.name
        programDescription = program.description
        programDuration = getTime(program.realDuration)
        programDurationSecs = program.duration
        realProgramDuration = program.realDuration
        programStartSecs = parseIsoDate(program.realStartAtIso) / 1000
        this.video = video
    }

    private fun getTime(secs: Int): String {
        val duration = Duration.standardSeconds(secs.toLong())
        val periodFormatter = if (duration.toStandardHours().hours > 0) {
            PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(1)
                .appendHours()
                .appendSuffix("h")
                .appendSeparator(" ")
                .appendMinutes()
                .appendSuffix("m")
        } else {
            PeriodFormatterBuilder()
                .appendMinutes()
                .appendSuffix("m")
        }
        return periodFormatter.toFormatter().print(duration.toPeriod())
    }

    fun isNotStream(): Boolean = video is YouTubeVideo || video is Mpeg4Video

    fun getSourceName(): String {
        val domainName = NetworkUtils.getDomainName(video?.getUrl()) ?: return "Showfer Media LLC"
        return if (video?.getPlayerType() == PlayerType.DAILYMOTION) {
            SOURCE_DAILYMOTION
        } else if (domainName.contains(DOMAIN_YOUTUBE)) {
            SOURCE_YOUTUBE
        } else if (domainName.contains(DOMAIN_ARCHIVE)){
            SOURCE_ARCHIVE
        } else {
            SOURCE_SHOWFER
        }
    }
}