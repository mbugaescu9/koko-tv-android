package com.kokoconnect.android.ui.fragment.vod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.databinding.FragmentContentSeriesBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.model.vod.Series
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.vod.SeriesViewModel
import javax.inject.Inject

class ContentSeriesFragment : Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.SERIES.defaultName
            this.type = ScreenType.SERIES
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private var binding: FragmentContentSeriesBinding? = null

    private val contentViewModel: VodContentViewModel by activityViewModels { viewModelFactory }
    private val seriesViewModel: SeriesViewModel by activityViewModels { viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContentSeriesBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        seriesViewModel.seriesLiveData.observe(viewLifecycleOwner, Observer { series ->
            if (series != null) {
                setSecondaryTitle(series.name)
                val collection = contentViewModel.currentCollection
                setPrimaryTitle(collection?.name)
                setCurrentSeries(series)
            }
        })
    }

    private fun setupViews() {
        binding?.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setCurrentSeries(series: Series) {
//        contentActivity?.setSecondaryTitle(series.name)
        navigationViewModel.setCurrentScreen(screen, this)
    }

    private fun setSecondaryTitle(title: String?) {
        binding?.tvTitleSecondary?.setText(title ?: "")
    }

    private fun setPrimaryTitle(title: String?) {
        if (title == null) {
            setActionBarExpanded(false)
        } else {
            setActionBarExpanded(true)
        }
        binding?.tvTitlePrimary?.setText(title ?: "")
    }

    fun setActionBarVisibile(isVisible: Boolean) {
        binding?.appbar?.visibility = if (!isVisible) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen, this)
    }

    fun setActionBarExpanded(isExpanded: Boolean, animated: Boolean = false) {
        binding?.appbar?.setExpanded(isExpanded, animated)
    }

    fun lockUi() {
        binding?.lockUiLayout?.visibility = View.VISIBLE
        binding?.lockUiLayout?.setOnClickListener {
            // ignore
        }
    }

    fun unlockUi() {
        binding?.lockUiLayout?.visibility = View.GONE
    }

}