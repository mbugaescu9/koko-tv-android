package com.kokoconnect.android.vh

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.databinding.ItemAdLayoutBinding
import com.kokoconnect.android.model.ads.banner.AdsObjectsProvider

class BannerViewHolder(
    private val binding: ItemAdLayoutBinding
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun buildFor(
            parent: ViewGroup,
            attachToRoot: Boolean = false,
            adsObjectsProvider: AdsObjectsProvider? = null
        ): BannerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemAdLayoutBinding.inflate(inflater, parent, attachToRoot)
            return BannerViewHolder(binding).apply {
                this.adsObjectsProvider = adsObjectsProvider
            }
        }
    }

    var isVisible: Boolean = false
        private set
    var isLoaded: Boolean = false
        private set

    private val bannerContainer = binding.adView

    fun setVisible(isVisible: Boolean) {
        if (isVisible) {
            visible()
        } else {
            hide()
        }
    }

    var adsObjectsProvider: AdsObjectsProvider? = null

    fun hide() {
        binding.expandableLayout.collapse()
        isVisible = false
    }

    fun visible() {
        binding.expandableLayout.expand()
        isVisible = true
        if (!isLoaded) {
            load()
        }
    }

    fun load() {
        val activity = adsObjectsProvider?.provideActivity()
        val bannerManager = adsObjectsProvider?.provideBannerManager()
        if (bannerManager != null && activity != null) {
            bannerManager.loadBannerInContainer(activity, bannerContainer)
            isLoaded = true
        }
    }
}