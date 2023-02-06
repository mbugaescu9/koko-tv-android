package com.kokoconnect.android.ui.fragment.vod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.vod.SeriesViewModel
import javax.inject.Inject

class SeriesFragment : Fragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val contentViewModel: VodContentViewModel by activityViewModels{ viewModelFactory }
    val seriesViewModel: SeriesViewModel by activityViewModels{ viewModelFactory }
    val navigationViewModel: NavigationViewModel by activityViewModels{ viewModelFactory }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_series, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return

        seriesViewModel.needOpenEpisode.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                seriesViewModel.needOpenEpisode.value = null
                contentViewModel.openContent(it)
                findNavController().navigateSafe(R.id.action_fragmentContentSeries_to_fragmentContent)
            }
        })
    }



    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        seriesViewModel.needShowSeriesDescription.postValue(true)
        seriesViewModel.needShowBanners.postValue(true)
        super.onResume()
    }
}