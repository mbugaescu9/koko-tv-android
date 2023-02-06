package com.kokoconnect.android.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentMainProfileBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.AmsEventsViewModel
import com.kokoconnect.android.vm.profile.ProfileViewModel
import com.kokoconnect.android.vm.profile.AuthViewModel
import javax.inject.Inject

class ProfileMainFragment: Fragment(), Injectable {
    companion object {
        const val tabId = 3
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val eventsModel: AmsEventsViewModel by activityViewModels { viewModelFactory }
    val authViewModel: AuthViewModel by activityViewModels { viewModelFactory }
    val profileViewModel: ProfileViewModel by activityViewModels { viewModelFactory }

//    var binding: FragmentMainProfileBinding? = null
    var binding: FragmentMainProfileBinding? = null
    var backPressedCallback: OnBackPressedCallback? = null
    private val navigationListener = NavController.OnDestinationChangedListener {
            controller, destination, arguments ->
//        val isStartDestination = destination.id == controller.graph.startDestination
        val isStartDestination = destination.id == controller.graph.startDestinationId
        binding?.llBack?.isVisible = !isStartDestination
        binding?.tvAppName?.isVisible = isStartDestination
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainProfileBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        authViewModel.isAuthorized.observe(viewLifecycleOwner, Observer {
            if (it == false) {
                profileViewModel.setProfile(null)
            }
        })
        authViewModel.needOpenAuth.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                authViewModel.needOpenAuth.value = null
                binding?.navHostFragment?.findNavController()?.navigateSafe(R.id.fragmentSignIn)
            }
        })

        backPressedCallback = activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            navigateBack()
        }
    }

    private fun setupViews() {
        binding?.llBack?.setOnClickListener {
            navigateBack()
        }
    }

    fun navigateBack() {
        val navController = binding?.navHostFragment?.findNavController()
        val currentDestinationId = navController?.currentDestination?.id ?: -1
        if (currentDestinationId >= 0 && currentDestinationId != R.id.fragmentProfile) {
            navController?.popBackStack()
        } else {
            backPressedCallback?.remove()
            activity?.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.navHostFragment?.findNavController()?.addOnDestinationChangedListener(navigationListener)
    }

    override fun onPause() {
        super.onPause()
        binding?.navHostFragment?.findNavController()?.removeOnDestinationChangedListener(navigationListener)
    }
}
