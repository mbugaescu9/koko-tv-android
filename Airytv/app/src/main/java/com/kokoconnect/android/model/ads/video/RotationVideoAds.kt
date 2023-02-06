package com.kokoconnect.android.model.ads.video

import android.content.Context
import com.kokoconnect.android.model.ads.Ima
import com.kokoconnect.android.model.ads.Infomercial

class RotationVideoAds(val context: Context,
                       val imaVastAds: List<Ima>?,
                       val infomercialAds: List<Infomercial>?) {

    private var imaIterator: Iterator<VideoAd>? = null
    private var infomercialIterator: Iterator<VideoAd>? = null
    private val imaVideoAds: ArrayList<VideoAd> =
        ArrayList<VideoAd>()
    private val infomercialVideoAds: ArrayList<VideoAd> =
        ArrayList<VideoAd>()

    init {
        for (ad in imaVastAds.orEmpty()) {
            imaVideoAds.add(
                ImaVastVideoAd(
                    ad,
                    context
                )
            )
        }
        imaVideoAds.sortedByDescending { it.priority() }
        for (ad in infomercialAds.orEmpty()) {
            infomercialVideoAds.add(
                InfomercialVideoAd(
                    ad
                )
            )
        }
        infomercialVideoAds.sortedByDescending { it.priority() }
    }

    fun isEmptyIma(): Boolean = imaVideoAds.isEmpty()
    fun isEmptyInfomercial(): Boolean = infomercialVideoAds.isEmpty()

    fun getNextIma(): VideoAd? {
        if (imaIterator?.hasNext() == true) {
            return imaIterator?.next()
        } else {
            if (!isEmptyIma()) {
                imaIterator = imaVideoAds.iterator()
                return imaIterator?.next()
            } else {
                return null
            }
        }
    }

    fun getNextInfomercial(): VideoAd? {
        if (infomercialIterator?.hasNext() == true) {
            return infomercialIterator?.next()
        } else {
            if (!isEmptyInfomercial()) {
                infomercialIterator = infomercialVideoAds.iterator()
                return infomercialIterator?.next()
            } else {
                return null
            }
        }
    }

}