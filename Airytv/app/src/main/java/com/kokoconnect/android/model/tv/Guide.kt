package com.kokoconnect.android.model.tv

import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.model.ads.video.*
import com.kokoconnect.android.model.player.*
import com.kokoconnect.android.model.player.VideoOpeningReason
import com.kokoconnect.android.util.DOMAIN_DAILYMOTION
import com.kokoconnect.android.util.DOMAIN_YOUTUBE
import com.kokoconnect.android.util.DateUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.annotations.SerializedName
import timber.log.Timber
import java.util.*

const val TYPE_ONLY_STREAM = 1
const val TYPE_REGULAR_CHANNEL = 2
const val TYPE_HLS_CHANNEL = 3

class ChannelsResponse {
    var categories: List<Category>? = null
    var advertisements: Map<String, ImaProgramParams>? = null
}

class Category(
    var name: String?,
    @SerializedName("channels")
    val regularChannels: List<Channel>?,
    @SerializedName("stream_channels")
    val streamChannels: List<Channel>?
) {
    fun channels(): List<Channel> {
        val array = LinkedList<Channel>()
        streamChannels?.forEach {
            it.type = if (it.hls) {
                TYPE_HLS_CHANNEL
            } else {
                TYPE_ONLY_STREAM
            }
            it.category = name
        }
        regularChannels?.forEach {
            it.type = TYPE_REGULAR_CHANNEL
            it.category = name
        }
        array.addAll(regularChannels.orEmpty())
        array.addAll(streamChannels.orEmpty())
        array.sortBy { it.number }
        return array
    }
}

class Channel(
    val id: Int,
    var name: String,
    var type: Int,
    val image: String,
    val hls: Boolean,
    val private: Boolean,
    @SerializedName("source_url")
    val sourceUrl: String?,
    val description: String,
    val number: Int,
    @SerializedName("broadcasts")
    val programs: List<Program>,
    var category: String?, // not delete
    var openingReason: VideoOpeningReason = VideoOpeningReason.ON_TV_CHANNEL_CHANGE
) {
    private var currentPart: Part? = null
    private var currentProgram: Program? = null

    fun getCurrentPart(): Part? {
        return currentPart
    }

    fun getCurrentProgram(): Program? {
        return currentProgram
    }

    fun close() {
        currentPart = null
    }

    fun switchToNextContent(): ProgramDescription? {
        if (currentPart != null) {
            var isFindCurrent = false
            for (program in this.programs.orEmpty()) {
                for (part in program.parts) {
                    if (part == currentPart) {
                        isFindCurrent = true
                        continue
                    }
                    if (isFindCurrent) {
                        currentPart = part
                        currentProgram = program
                        val video = parseProgramPart(program, part, 0)
                        val reason = if (part == program.parts.first()) {
                            VideoOpeningReason.ON_TV_PROGRAM_CHANGE
                        } else {
                            VideoOpeningReason.ON_TV_PROGRAM_PART_CHANGE
                        }
                        val description = ProgramDescription(video, program, this)
                        description.videoOpeningReason = reason
                        Timber.d("Who called requestOpenProgram(): getNextChannelContent()")
                        return description
                    }
                }
            }
        }
        return null
    }

    fun switchToCurrentContent(): ProgramDescription? {
        Timber.d("getCurrentProgram() ${type}")
        when (type) {
            TYPE_ONLY_STREAM -> {
                val program = parseCurrentProgram()
                currentProgram = program
                val video =
                    YouTubeStream(sourceUrl!!, DateUtils.parseIsoDate(program.realStartAtIso))
                return ProgramDescription(video, program, this)
            }
            TYPE_REGULAR_CHANNEL -> {
                val program = parseCurrentProgram()
                currentProgram = program
                if (!program.sourceUrl.isNullOrEmpty()) {
                    val video = YouTubeStream(
                        program.sourceUrl,
                        DateUtils.parseIsoDate(program.realStartAtIso)
                    )
                    return ProgramDescription(video, program, this)
                } else if (!program.parts.isNullOrEmpty()) {
                    val part = program.getCurrentPart()
                    val seekTo = part.getCurrentPositionMs()
                    currentPart = part
                    val video = parseProgramPart(program, part, seekTo)
                    return ProgramDescription(video, program, this)
                } else {
                    val info = "program.parts.isNullOrEmpty() - " +
                            "amsId: ${Preferences(AiryTvApp.instance).Ams().getAmsId()}" +
                            "channel: ${this.number}: ${this.name}\n" +
                            "program: ${program.name}\n" +
                            "startTime: ${android.text.format.DateUtils.formatElapsedTime(DateUtils.getCurrentTime() / 1000)}\n"
                    FirebaseCrashlytics.getInstance().recordException(NullPointerException(info))
                }
            }
            TYPE_HLS_CHANNEL -> {
                val program = parseCurrentProgram()
                currentProgram = program
                val video =
                    HlsStream(this.sourceUrl!!, DateUtils.parseIsoDate(program.realStartAtIso))
                return ProgramDescription(video, program, this)
            }
        }
        return null
    }

    private fun parseProgramPart(program: Program, part: Part, seekTo: Long): PlayerObject? {
        if (part.sourceUrl.contains(DOMAIN_YOUTUBE)) {
            return YouTubeVideo(
                part.sourceUrl,
                seekTo,
                DateUtils.parseIsoDate(program.realStartAtIso)
            )
        } else if (part.sourceUrl.contains(DOMAIN_DAILYMOTION)) {
            return DailymotionVideo(
                part.sourceUrl,
                seekTo,
                DateUtils.parseIsoDate(program.realStartAtIso)
            )
        } else {
            return Mpeg4Video(
                part.sourceUrl,
                seekTo,
                DateUtils.parseIsoDate(program.realStartAtIso),
                this.private
            )
        }
    }

    fun parseCurrentProgram(): Program {
        for ((index, program) in programs.withIndex()) {
            if (index != programs.lastIndex) {
                val nextProgram = programs.get(index + 1)
                val checkFirst =
                    DateUtils.parseIsoDate(program.realStartAtIso) <= DateUtils.getCurrentDate()
                val checkSecond =
                    DateUtils.parseIsoDate(nextProgram.realStartAtIso) >= DateUtils.getCurrentDate()
                if (checkFirst && checkSecond) {
                    return program
                }
            }
        }
        return programs.last()
    }

    fun parseNextProgram(): Program? {
        for ((index, program) in programs.withIndex()) {
            if (index != programs.lastIndex) {
                val nextProgram = programs.get(index + 1)
                val checkFirst =
                    DateUtils.parseIsoDate(program.realStartAtIso) <= DateUtils.getCurrentDate()
                val checkSecond =
                    DateUtils.parseIsoDate(nextProgram.realStartAtIso) >= DateUtils.getCurrentDate()
                if (checkFirst && checkSecond) {
                    return nextProgram
                }
            }
        }
        return null
    }
}

/*
    real == stream
    start == view
 */
class Program(
    val id: Int,
    @SerializedName("title")
    val name: String,
    @SerializedName("source_url")
    val sourceUrl: String?,
    @SerializedName("stream_duration")
    val realDuration: Int,
    @SerializedName("view_duration")
    val duration: Int,
    val description: String,
    val startAt: String,
    @SerializedName("blockAds")
    var videoAdBlocks: List<ImaProgramAdBlock>?,
//    @SerializedName("stream_start_at")
//    val realStartAt: String,
    @SerializedName("stream_start_at_iso")
    val realStartAtIso: String,
    @SerializedName("parts")
    val parts: List<Part>
) {
    fun getCurrentPart(): Part {
        for ((index, part) in parts.withIndex()) {
            if (index != parts.lastIndex) {
                val nextPart = parts.get(index + 1)
                val checkFirst =
                    DateUtils.parseIsoDate(part.startAtIso) <= DateUtils.getCurrentDate()
                val checkSecond =
                    DateUtils.parseIsoDate(nextPart.startAtIso) >= DateUtils.getCurrentDate()
                if (checkFirst && checkSecond) {
                    return part
                }
            }
        }
        return parts.last()
    }

    fun getNextPart(): Part? {
        for ((index, part) in parts.withIndex()) {
            if (index != parts.lastIndex) {
                val nextPart = parts.get(index + 1)
                val checkFirst =
                    DateUtils.parseIsoDate(part.startAtIso) <= DateUtils.getCurrentDate()
                val checkSecond =
                    DateUtils.parseIsoDate(nextPart.startAtIso) >= DateUtils.getCurrentDate()
                if (checkFirst && checkSecond) {
                    return nextPart
                }
            }
        }
        return null
    }


    fun getImaAds(): ImaProgramAds? {
        // for testing ad blocks
//        val videoAdBlocks = (0..100).mapNotNull { idx ->
//            ImaProgramAdBlock().apply {
//                this.title = "AdBlock 1"
//                this.ads = listOf("publisherdesk.ima")
//                this.offset = idx * 100L
//                this.needPlay = true
//            }
//        }
        val videoAdBlocks = videoAdBlocks ?: return null

        return if (videoAdBlocks.isNotEmpty()) {
            val adTags = mutableListOf<String>()
            videoAdBlocks.forEach {
                it.ads?.forEach {
                    if (!adTags.contains(it)) {
                        adTags.add(it)
                    }
                }
            }
            val ads = adTags.map { Pair<String, ImaProgram?>(it, null) }.toMap().toMutableMap()
            ImaProgramAds(
                ads = ads,
                blocks = videoAdBlocks
            )
        } else {
            null
        }
    }
}

class Part(
    val id: Int,
    @SerializedName("source_url")
    val sourceUrl: String,
    @SerializedName("start_at_iso")
    val startAtIso: String
) {
    fun getCurrentPositionMs(): Long {
        return (DateUtils.getCurrentDate() - DateUtils.parseIsoDate(startAtIso))
    }
}