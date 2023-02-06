package com.kokoconnect.android.ui.fragment.tv

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentDescriptionBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.model.tv.ProgramDescription
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.util.IntentUtils
import com.kokoconnect.android.util.isOrientationPortrait
import com.kokoconnect.android.vm.tv.TvGuideViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import org.jetbrains.anko.contentView
import timber.log.Timber
import javax.inject.Inject


class DescriptionFragment : Fragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val tvGuideViewModel: TvGuideViewModel by activityViewModels { viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }
    private var popupDescription: PopupWindow? = null

    var binding: FragmentDescriptionBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDescriptionBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        tvGuideViewModel.currentDescriptionLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding?.textViewProgramName?.text = it.programName
                if (it.programDescription?.isNotBlank() == true) {
                    binding?.textViewProgramDescription?.visibility = View.VISIBLE
                    binding?.textViewProgramDescription?.text = it.programDescription
                } else {
                    binding?.textViewProgramDescription?.visibility = View.GONE
                }
                binding?.textViewSourceNameMain?.text = getContentSource(it)
                binding?.textViewSourceName?.text = getContentSource(it)
                binding?.textViewProgramDuration?.text = it.programDuration
            }
        })
        tvGuideViewModel.isPopupDescriptionVisible.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    openPopupDescription()
                } else {
                    popupDescription?.dismiss()
                }
            }
        })
        tvGuideViewModel.programProgress.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding?.programProgress?.progress = it.first
                binding?.programProgress?.max = it.second
            }
        })
        view?.setOnClickListener {
            setDescriptionVisible(false)
        }
    }

    private fun setupViews() {
        binding?.buttonShare?.setOnClickListener {
            shareChannel()
        }
        binding?.buttonFavourite?.setOnClickListener {
            favouriteChannel()
        }
    }

    private fun shareChannel() {
        navigationViewModel.createShareChannelLink(tvGuideViewModel.getChannel())?.let { link ->
            Timber.d("share link ${link}")
            val shareLinkIntent = IntentUtils.getShareLinkIntent(link.toString())
            startActivity(Intent.createChooser(shareLinkIntent, "Share link"))
        }
    }

    private fun favouriteChannel() {
        Toast.makeText(context, R.string.added_to_favourites, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        binding?.buttonSwitchMode?.setOnClickListener {
            Timber.d("buttonSwitchMode() ${binding?.layoutMoreInfo?.visibility}")
            setDescriptionVisible(binding?.layoutMoreInfo?.visibility != View.VISIBLE)
        }
        super.onResume()
    }

    private fun setDescriptionVisible(visible: Boolean) {
        if (visible) {
            view?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding?.textViewSourceNameMain?.visibility = View.GONE
            binding?.layoutMoreInfo?.visibility = View.VISIBLE
            binding?.textViewProgramName?.maxLines = 2
            tvGuideViewModel.setPopupDescriptionVisible(true)
        } else {
            view?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            binding?.textViewSourceNameMain?.visibility = View.VISIBLE
            binding?.layoutMoreInfo?.visibility = View.GONE
            binding?.textViewProgramName?.maxLines = 1
            tvGuideViewModel.setPopupDescriptionVisible(false)
        }
    }

    private fun convertDpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context?.resources?.displayMetrics
        ).toInt()
    }

    private fun openPopupDescription() {
        val isPortrait = resources.configuration.isOrientationPortrait()
        if (isPortrait && context != null && activity?.contentView?.windowToken != null) {
            val customView = LayoutInflater.from(context).inflate(R.layout.popup_description, null)
            popupDescription?.dismiss()
            popupDescription = PopupWindow(
                customView,
                view?.width ?: convertDpToPx(284),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
            )
            if (Build.VERSION.SDK_INT >= 21) {
                popupDescription?.setElevation(5.0f)
            }
//            popupDescription?.isOutsideTouchable = true
            popupDescription?.showAtLocation(
                activity?.contentView, Gravity.NO_GRAVITY, convertDpToPx(35),
                ActivityUtils.getStatusBarHeight(activity) + (view?.height ?: convertDpToPx(50))
            )
            val observer = Observer<ProgramDescription?> {
                if (it != null) {
                    val textViewProgramDescription =
                        customView.findViewById<TextView>(R.id.textViewProgramDescription)
                    val textViewSourceName =
                        customView.findViewById<TextView>(R.id.textViewSourceName)
                    val textViewProgramDuration =
                        customView.findViewById<TextView>(R.id.textViewProgramDuration)
                    if (it.programDescription?.isNotBlank() == true) {
                        textViewProgramDescription?.visibility = View.VISIBLE
                        textViewProgramDescription?.text = it.programDescription
                    } else {
                        textViewProgramDescription?.visibility = View.GONE
                    }
                    binding?.textViewSourceNameMain?.visibility = View.INVISIBLE
                    textViewSourceName?.text = getContentSource(it)
                    textViewProgramDuration?.text = it.programDuration
                }
            }
            tvGuideViewModel.currentDescriptionLiveData.observe(viewLifecycleOwner, observer)
            popupDescription?.setOnDismissListener {
                binding?.textViewSourceNameMain?.visibility = View.VISIBLE
                tvGuideViewModel.currentDescriptionLiveData.removeObserver(observer)
            }
        }
    }

    fun getContentSource(programDescription: ProgramDescription?): String? {
        val sourceName = programDescription?.getSourceName() ?: return ""
        return context?.getString(R.string.content_source, sourceName) ?: ""
    }

}
