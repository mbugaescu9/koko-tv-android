package com.kokoconnect.android.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentSuggestionFeedbackSelectBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.ui.fragment.BaseFragment
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.NavigationViewModel
import javax.inject.Inject

class SuggestionFeedbackSelectFragment: BaseFragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.SUGGESTION.defaultName
            this.type = ScreenType.SUGGESTION
        }
    }

    private var binding: FragmentSuggestionFeedbackSelectBinding? = null

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSuggestionFeedbackSelectBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

    }

    private fun setupViews() {
        binding?.flContentSuggestion?.setOnClickListener {
            findNavController().navigateSafe(R.id.action_fragmentSuggestionFeedbackSelect_to_fragmentContentSuggestion)
        }
        binding?.flTechnicalAssistance?.setOnClickListener {
            findNavController().navigateSafe(R.id.action_fragmentSuggestionFeedbackSelect_to_fragmentTechnicalAssistance)
        }
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen)
    }
}