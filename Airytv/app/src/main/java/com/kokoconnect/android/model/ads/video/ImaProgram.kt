package com.kokoconnect.android.model.ads.video

import com.kokoconnect.android.model.ads.Ima
import org.joda.time.Duration

/** program-specific ad from channels response **/
/** timestamps - ima time offset in millis **/
class ImaProgram(
    name: String? = null,
    priority: Int? = null,
    payload: String? = null
) : Ima(priority, payload) {

    init {
        this.name = name
    }

    fun setParams(params: ImaProgramParams) {
        this.priority = params.priority ?: 0
        this.payload = params.payload
    }

    override fun toString(): String {
        return "ImaProgram: tag = ${name} priority = ${priority} payload = ${payload}"
    }
}

class ImaProgramAdBlock {
    var title: String? = null
    var ads: List<String>? = null
    var offset: Long = 0
    val timestampMs: Long by lazy {
        Duration.standardSeconds(offset).millis
    }
    var needPlay: Boolean = true
}

class ImaProgramParams {
    var priority: Int? = null
    var payload: String? = null
}

class ImaProgramAds(
    val ads: MutableMap<String, ImaProgram?>,
    val blocks: List<ImaProgramAdBlock>
) {
    companion object {
        private const val DELTA_MS = 1500
    }

    private val emptySetupVideoAd = EmptySetupVideoAd()
    private val setupVideoAd = object : SetupVideoAd {
        override fun imaVast(): SetupItemVideoAd {
            return object : SetupItemVideoAd {
                override fun enable(): Boolean = ads.isNotEmpty()
                override fun numberOfAdsToServe(): Int = 1
                override fun numberOfChannels(): Int = IGNORE_INT
                override fun repeatIntervalSec(): Long = IGNORE_LONG
            }
        }

        override fun infomercial(): SetupItemVideoAd {
            return EmptySetupItemVideoAd()
        }

        override fun isAnyEnabled(): Boolean {
            return imaVast().enable() || infomercial().enable()
        }
    }

    fun getSetupVideoAd(videoAdTrigger: VideoAdTrigger): SetupVideoAd {
        return when (videoAdTrigger) {
            VideoAdTrigger.OnTvProgramTimestamp -> setupVideoAd
            else -> emptySetupVideoAd
        }
    }

    fun setParams(params: Map<String, ImaProgramParams>?) {
        params?.forEach {
            if (ads.containsKey(it.key)) {
                ads[it.key] = ImaProgram(it.key).apply {
                    setParams(it.value)
                }
            }
        }
    }

    fun getCurrentAdBlock(positionMs: Long): ImaProgramAdBlock? {
        return blocks.find {
            val isCurrent =
                it.needPlay && (positionMs in (it.timestampMs)..(it.timestampMs + DELTA_MS))
            if (isCurrent) it.needPlay = false
            isCurrent
        }
    }

    fun getCurrentAds(positionMs: Long): List<ImaProgram>? {
        return getCurrentAds(getCurrentAdBlock(positionMs))
    }

    fun getCurrentAds(adBlock: ImaProgramAdBlock?): List<ImaProgram>? {
        return adBlock?.ads?.mapNotNull {
            ads[it]?.apply {
                extra = adBlock.title
            }
        }
    }

    fun getAllAds(): List<Ima>? {
        return ads.mapNotNull {
            it.value
        }
    }
}