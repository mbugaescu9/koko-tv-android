package com.kokoconnect.android.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kokoconnect.android.R
import com.kokoconnect.android.model.ui.Page
import com.kokoconnect.android.ui.fragment.tv.ChannelsMainFragment
import com.kokoconnect.android.ui.fragment.freegift.GiveawaysMainFragment
import com.kokoconnect.android.ui.fragment.profile.ProfileMainFragment
import com.kokoconnect.android.ui.fragment.vod.VodMainFragment

class MainFragmentPagerAdapter(
    context: Context?,
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {
    private var pages = listOf<Page>(
        Page(context?.getString(R.string.tv) ?: "", ChannelsMainFragment.tabId),
        Page(context?.getString(R.string.vod) ?: "", VodMainFragment.tabId),
        Page(context?.getString(R.string.free_gift) ?: "", GiveawaysMainFragment.tabId),
        Page(context?.getString(R.string.profile) ?: "", ProfileMainFragment.tabId)
    )

    fun getTabTitle(position: Int): String {
        return pages.get(position).title
    }

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (pages.get(position).fragmentId) {
            ChannelsMainFragment.tabId -> {
                ChannelsMainFragment()
            }
            VodMainFragment.tabId -> {
                VodMainFragment()
            }
            GiveawaysMainFragment.tabId -> {
                GiveawaysMainFragment()
            }
            else -> {
                ProfileMainFragment()
            }
        }
    }
}