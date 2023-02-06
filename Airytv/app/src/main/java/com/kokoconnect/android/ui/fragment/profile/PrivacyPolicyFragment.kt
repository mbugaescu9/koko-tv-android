package com.kokoconnect.android.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.kokoconnect.android.databinding.FragmentPrivacyPolicyBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.ui.fragment.BaseFragment
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.vm.NavigationViewModel
import javax.inject.Inject


class PrivacyPolicyFragment: BaseFragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.PRIVACY_POLICY.defaultName
            this.type = ScreenType.PRIVACY_POLICY
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private var binding: FragmentPrivacyPolicyBinding? = null
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var html = readFileText("privacy_policy_text.txt")
        html += "\nBuild number: ${AppParams.versionName} (${AppParams.versionCode}) (api ${AppParams.apiVersion})"
        binding?.webView?.loadDataWithBaseURL(AppParams.serverApiUrl, html, "text/html", "UTF-8", null)
    }

    private fun readFileText(fileName: String): String {
        return context?.assets?.open(fileName)?.bufferedReader().use { it?.readText() } ?: ""
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(ProfileFragment.screen, this)
    }
}