package com.kokoconnect.android.model.item

import com.kokoconnect.android.R
import com.kokoconnect.android.model.ads.banner.BannerManager
import com.kokoconnect.android.model.tv.Channel

sealed class GuideRow (val rowType: GuideRowEnum)
class GuideRowCategory(val name: String?) : GuideRow(
    GuideRowEnum.GUIDE_ROW_CATEGORY
)
class GuideRowChannel(val channel: Channel) : GuideRow(
    GuideRowEnum.GUIDE_ROW_CHANNEL
)

class GuideRowBanner(val bannerManager: BannerManager) : GuideRow(
    GuideRowEnum.GUIDE_ROW_BANNER
)

enum class GuideRowEnum(val layoutId: Int, val layoutIdNew: Int) {
    GUIDE_ROW_CATEGORY(R.layout.item_guide_category, R.layout.item_guide_category),
    GUIDE_ROW_CHANNEL(R.layout.item_guide_channel_layout, R.layout.item_guide_channel_layout),
    GUIDE_ROW_BANNER(R.layout.item_ad_layout, R.layout.item_ad_layout)
}
