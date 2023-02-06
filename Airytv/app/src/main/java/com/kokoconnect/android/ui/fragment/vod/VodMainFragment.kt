package com.kokoconnect.android.ui.fragment.vod

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentMainVodBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.ui.activity.BaseActivity
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.isOrientationLandscape
import com.kokoconnect.android.vm.NavigationViewModel
import com.google.android.gms.cast.framework.CastButtonFactory
import javax.inject.Inject

class VodMainFragment() : Fragment(), Injectable {
    companion object {
        const val tabId = 1
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    var binding: FragmentMainVodBinding? = null
    var backPressedCallback: OnBackPressedCallback? = null
    private val navigationListener =
        NavController.OnDestinationChangedListener { controller, destination, arguments ->
//            val isStartDestination = destination.id == controller.graph.startDestination
            val isStartDestination = destination.id == controller.graph.startDestinationId
            binding?.llBack?.isVisible = !isStartDestination
            binding?.tvAppName?.isVisible = isStartDestination
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainVodBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding?.navHostFragment?.findNavController()?.let {
//            navigationViewModel.setNavigationController(it)
//        }
        setupViews()

        backPressedCallback = activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            navigateBack()
        }
    }

    private fun setupViews() {
        val chromecastActivity = activity as? BaseActivity
        val btnMediaRoute = binding?.btnMediaRoute
        if (AppParams.isChromecastVodEnabled) {
            btnMediaRoute?.visibility = View.VISIBLE
            if (chromecastActivity != null && btnMediaRoute != null) {
                CastButtonFactory.setUpMediaRouteButton(chromecastActivity, btnMediaRoute)
            }
        } else {
            btnMediaRoute?.visibility = View.INVISIBLE
        }
        binding?.llBack?.setOnClickListener {
            navigateBack()
        }
        updateOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    private fun updateOrientation(isLandscape: Boolean = context?.isOrientationLandscape() == true) {
        binding?.appbar?.isVisible = !isLandscape
    }

    fun navigateBack() {
        val navController = binding?.navHostFragment?.findNavController()
        val currentDestinationId = navController?.currentDestination?.id ?: -1
        if (currentDestinationId >= 0 && currentDestinationId != R.id.fragmentContentAll) {
            navController?.popBackStack()
        } else {
            backPressedCallback?.remove()
            activity?.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.navHostFragment?.findNavController()
            ?.addOnDestinationChangedListener(navigationListener)
    }

    override fun onPause() {
        super.onPause()
        binding?.navHostFragment?.findNavController()
            ?.removeOnDestinationChangedListener(navigationListener)
    }
}

