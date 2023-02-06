package com.kokoconnect.android.ui.fragment.freegift

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.MenuListAdapter
import com.kokoconnect.android.databinding.FragmentRecyclerListBinding
import com.kokoconnect.android.model.ui.MenuItem
import com.kokoconnect.android.ui.activity.WebViewActivity
import org.jetbrains.anko.support.v4.startActivity
import timber.log.Timber


class GiveawaysRulesFragment : Fragment() {
    companion object {
        const val RULES_MENU_FAQ_ID = 0
        const val RULES_MENU_RULES_ID = 1
    }

    private var recyclerLayoutManager: RecyclerView.LayoutManager? = null
    private var rulesMenuItems: List<MenuItem> = emptyList()

    private val rulesListener = object : MenuListAdapter.Listener {
        override fun onClick(item: MenuItem) {
            openMenuItem(item)
        }
    }
    private var binding: FragmentRecyclerListBinding? = null

    private var recyclerAdapter: MenuListAdapter? = null
        set(value) {
            binding?.recyclerView?.adapter = value
            if ((value?.itemCount ?: 0) > 0) {
                binding?.emptyMessage?.visibility = View.GONE
            } else {
                binding?.emptyMessage?.visibility = View.VISIBLE
            }
            field = value
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecyclerListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerLayoutManager?.let {
            binding?.recyclerView?.layoutManager = it
        }
        recyclerAdapter?.let {
            binding?.recyclerView?.adapter = it
        }
        initRulesMenu()
    }

    private fun initRulesMenu() {
        rulesMenuItems = listOf(
            MenuItem(
                RULES_MENU_FAQ_ID,
                getString(R.string.giveaways_rules_faq_name),
                R.drawable.ic_faq,
                getString(R.string.giveaways_rules_faq_description)
            ),
            MenuItem(
                RULES_MENU_RULES_ID,
                getString(R.string.giveaways_rules_rules_name),
                R.drawable.ic_rules,
                getString(R.string.giveaways_rules_rules_description)
            )
        )
        recyclerAdapter = MenuListAdapter(rulesMenuItems, rulesListener)
    }

    private fun openMenuItem(item: MenuItem) {
        Timber.d("openMenuItem")
        var resId = 0
        val argsList = mutableListOf<String?>()
        when (item.id) {
            RULES_MENU_FAQ_ID -> {
                resId = R.string.giveaways_faq_text
            }
            RULES_MENU_RULES_ID -> {
                resId = R.string.giveaways_rules_text
            }
        }
        val argsString = argsList.joinToString(WebViewActivity.ARGS_SEPARATOR)
        //launch activity with text from resources
        //if there are args in resources string, we must put this args to single string with separators
        activity?.apply {
            startActivity<WebViewActivity>(
                WebViewActivity.KEY_RESOURCE_ID to resId,
                WebViewActivity.KEY_ARGS to argsString
            )
        }
    }
}