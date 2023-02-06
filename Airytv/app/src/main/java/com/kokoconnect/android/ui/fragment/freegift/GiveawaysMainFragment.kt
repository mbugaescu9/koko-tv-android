package com.kokoconnect.android.ui.fragment.freegift

import android.app.ActionBar
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.GiveawaysAdapter
import com.kokoconnect.android.databinding.DialogGiveawayInfoBinding
import com.kokoconnect.android.databinding.FragmentMainGiveawaysBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.model.ads.AdPurpose
import com.kokoconnect.android.model.ads.interstitial.InitInterstitial
import com.kokoconnect.android.model.ads.interstitial.InterstitialTrigger
import com.kokoconnect.android.model.ads.interstitial.rewarded.RewardedListener
import com.kokoconnect.android.model.error.GiveawaysError
import com.kokoconnect.android.model.error.GiveawaysErrorType
import com.kokoconnect.android.model.giveaways.GiveawaysItem
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.ui.dialog.CustomDialog
import com.kokoconnect.android.util.FirebaseLogger
import com.kokoconnect.android.util.resumeWithSafe
import com.kokoconnect.android.util.viewBinding
import com.kokoconnect.android.vm.AdsViewModel
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.freegift.GiveawaysViewModel
import com.kokoconnect.android.vm.profile.AuthViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject

class GiveawaysMainFragment : Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.FREE_GIFT.defaultName
            this.type = ScreenType.FREE_GIFT
        }
        const val tabId = 2
    }

    val binding by viewBinding(FragmentMainGiveawaysBinding::inflate)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val giveawaysViewModel: GiveawaysViewModel by activityViewModels { viewModelFactory }
    private val authViewModel: AuthViewModel by activityViewModels { viewModelFactory }
    private val adsViewModel: AdsViewModel by activityViewModels { viewModelFactory }
    private val eventsModel: AmsEventsViewModel by activityViewModels { viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    var adapter: GiveawaysAdapter? = null
    val adapterListener = object : GiveawaysAdapter.Listener {
        override fun onAddTicket(item: GiveawaysItem) {
            postEntryToGiveaway(item)
        }

        override fun onViewWinner(item: GiveawaysItem) {
            item.isWinnerVisible = true
            adapter?.updateAll()
        }

        override fun getUserAvatarUrl(): String? {
            return null
        }
    }
    private var currentItem: GiveawaysItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        giveawaysViewModel.apply {
            allGiveaways.observe(viewLifecycleOwner, Observer {
                updateGiveaways(it)
            })
            giveawaysInfoLiveData.observe(viewLifecycleOwner, Observer {
                binding?.tvAvailableTicketsCount?.text = it.available.toString()
            })
            giveawaysErrorLiveData.observe(viewLifecycleOwner, Observer {
                it?.let {
                    giveawaysViewModel.giveawaysErrorLiveData.value = null
                    showError(it)
                }
            })
        }
    }

    private fun setupViews() {
        adapter = GiveawaysAdapter(adapterListener)
        binding?.rvGiveaways?.adapter = adapter
        binding?.rvGiveaways?.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding?.tvGetMoreTickets?.setOnClickListener {
            showAdAndGetTicket()
        }
    }

    private fun postEntryToGiveaway(item: GiveawaysItem) {
        lifecycleScope.launch {
            currentItem = item
            setLockUiVisible(true)
            val entered = giveawaysViewModel.postGiveawaysEntry(item)
            if (entered) {
                onTicketAdded()
            }
            setLockUiVisible(false)
        }
    }

    private fun onTicketAdded() {
        val context = context ?: return
        val currentItemName = currentItem?.name ?: ""
        val currentItemId = currentItem?.id ?: 0
        val amsId = Preferences(context).Ams().getAmsId() ?: ""
        FirebaseLogger.logGiveawaysClickEvent(amsId, currentItemId)
        eventsModel.sendGiveawaysEventClickedEnter(currentItemName)
        Toast.makeText(
            context,
            "Congratulations! You will be in draw for '$currentItemName'",
            Toast.LENGTH_LONG
        ).show()
        requestGiveaways()
    }

    private fun showAdAndGetTicket() {
        lifecycleScope.launch {
            setLockUiVisible(true)
            val limit = giveawaysViewModel.giveawaysInfoLiveData.value?.limit ?: 0
            val count = giveawaysViewModel.giveawaysInfoLiveData.value?.count ?: 0

            val authorised =
                authViewModel.isAuthorized() && authViewModel.checkToken(authViewModel.getToken())
            if (!authorised) {
                giveawaysViewModel.setGiveawaysError(GiveawaysError(GiveawaysErrorType.ERROR_NOT_AUTHORIZED))
            } else if (count >= limit) {
                giveawaysViewModel.setGiveawaysError(GiveawaysError(GiveawaysErrorType.ERROR_ENTRY_LIMIT_REACHED))
            } else {
                val adsShown = suspendCancellableCoroutine<Boolean> { continuation ->
                    adsViewModel.showAdIfReady(
                        activity,
                        InterstitialTrigger.OnGetMoreTickets,
                        listOf(AdPurpose.GIVEAWAYS),
                        object : InitInterstitial.FinishListener {
                            override fun onNotLoadedAds() {
                                Timber.d("showAdIsReady: onNotLoadedAds()")
                                continuation.resumeWithSafe(Result.success(false))
                            }

                            override fun onFinish() {
                                Timber.d("showAdIsReady: onFinish()")
                                continuation.resumeWithSafe(Result.success(true))
                            }
                        },
                        object : RewardedListener {
                            override fun onRewarded() {
                                Timber.d("showAdIsReady: onRewarded()")
                            }
                        })
                }
                if (adsShown) {
                    giveawaysViewModel.getGiveawaysTicket()
                } else {
                    giveawaysViewModel.setGiveawaysError(
                        GiveawaysError(
                            GiveawaysErrorType.ERROR_AD_NOT_AVAILABLE
                        )
                    )
                }
            }
            setLockUiVisible(false)
        }
    }

    private fun updateGiveaways(allGiveaways: List<GiveawaysItem>) {
        adapter?.items = allGiveaways
        if (allGiveaways.isEmpty()) {
            binding?.flEmptyMessage?.visibility = View.VISIBLE
            binding?.rvGiveaways?.visibility = View.INVISIBLE
        } else {
            binding?.flEmptyMessage?.visibility = View.GONE
            binding?.rvGiveaways?.visibility = View.VISIBLE
        }
    }

    private fun showError(error: GiveawaysError) {
        context ?: return
        setLockUiVisible(false)

        when (error.type) {
            GiveawaysErrorType.ERROR_REQUEST_ACTIVE_LIST -> {
                val errorException = error.exception
                if (errorException is ApiErrorThrowable) {
                    val errorMessage = when (errorException.errorType) {
                        ApiError.NETWORK_PROBLEM -> {
                            getString(R.string.error_network_problem)
                        }
                        ApiError.SERVER_ERROR -> {
                            getString(R.string.error_server_error)
                        }
                        ApiError.SERVER_RESULT_NOT_200 -> {
                            "${getString(R.string.error_server_error_code)}, ${errorException.errorType.code}"
                        }
                        else -> {
                            null
                        }
                    }
                    errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            GiveawaysErrorType.ERROR_REQUEST_COMPLETED_LIST -> {
                val errorException = error.exception
                if (errorException is ApiErrorThrowable) {
                    val errorMessage = when (errorException.errorType) {
                        ApiError.NETWORK_PROBLEM -> {
                            getString(R.string.error_network_problem)
                        }
                        ApiError.SERVER_ERROR -> {
                            getString(R.string.error_server_error)
                        }
                        ApiError.SERVER_RESULT_NOT_200 -> {
                            "${getString(R.string.error_server_error_code)}, ${errorException.errorType.code}"
                        }
                        else -> {
                            null
                        }
                    }
                    errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            GiveawaysErrorType.ERROR_GET_TICKET -> {

            }
            GiveawaysErrorType.ERROR_ENTRY_LIMIT_REACHED -> {
                val context = context ?: return
                val limit = giveawaysViewModel.giveawaysInfoLiveData.value?.limit ?: 0
                CustomDialog.Builder()
                    .setMessage(getString(R.string.giveaways_entry_error_text, limit))
                    .setOkButtonText(getString(R.string.giveaways_entry_error_cancel))
                    .build(context)
                    .show()
            }
            GiveawaysErrorType.ERROR_NOT_AUTHORIZED -> {
                openAuth()
            }
            GiveawaysErrorType.ERROR_AD_NOT_AVAILABLE -> {
                val context = context ?: return
                CustomDialog.Builder()
                    .setMessage(getString(R.string.giveaways_noads_error_text))
                    .setOkButtonText(getString(R.string.giveaways_noads_error_cancel))
                    .build(context)
                    .show()
            }

            else -> null
        }
    }

    fun setLockUiVisible(isVisible: Boolean) {
        binding?.lockUi?.root?.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding?.lockUi?.root?.setOnClickListener {
            // ignore
        }
    }

    private fun showGiveawaysInfo() {
        val context = context ?: return
        if (Preferences(context).Giveaways().isNeedInfoDialog()) {
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(
                    getString(R.string.giveaways_info_dialog),
                    Html.FROM_HTML_MODE_COMPACT
                ).toSpannable()
            } else {
                Html.fromHtml(getString(R.string.giveaways_info_dialog)).toSpannable()
            }
            var dialogInfo: AlertDialog? = null
            val decorView = activity?.window?.decorView as? ViewGroup
            val dialogBuilder = AlertDialog.Builder(context)
            val view = layoutInflater.inflate(R.layout.dialog_giveaway_info, decorView, false)
            val dialogBinding = DialogGiveawayInfoBinding.bind(view)
            dialogBinding?.giveawaysInfoText?.setText(message)
            dialogBinding?.buttonContinue?.setOnClickListener {
                dialogInfo?.cancel()
            }
            dialogBuilder.setView(view)
            dialogInfo = dialogBuilder.create()
            dialogInfo?.setOnCancelListener {
                Preferences(context).Giveaways().setNeedInfoDialog(false)
            }
            dialogInfo?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogInfo?.show()
            dialogInfo?.window?.setLayout(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun openAuth() {
        Toast.makeText(context, getString(R.string.please_authorise_first), Toast.LENGTH_LONG)
            .show()
        authViewModel.openAuth()
    }

    private fun requestGiveaways() {
        lifecycleScope.launch {
            setLockUiVisible(true)
            giveawaysViewModel.requestGiveaways()
            setLockUiVisible(false)
        }
    }

    override fun onResume() {
        navigationViewModel.setCurrentScreen(screen, this)
//        showGiveawaysInfo()
        requestGiveaways()
        super.onResume()
    }

//    override fun finish() {
//        super.finish()
//        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
//    }

}
