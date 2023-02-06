package com.kokoconnect.android.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentContentSuggestionBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.error.FeedbackError
import com.kokoconnect.android.model.error.FeedbackErrorType
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.ui.fragment.BaseFragment
import com.kokoconnect.android.vm.profile.ProfileViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContentSuggestionFragment: BaseFragment(), Injectable {
    var binding: FragmentContentSuggestionBinding? = null

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val profileViewModel: ProfileViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContentSuggestionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        profileViewModel.feedbackError.observe(viewLifecycleOwner, Observer {
            it?.let {
                profileViewModel.feedbackError.value = null
                showError(it)
            }
        })
    }

    private fun setupViews() {
        binding?.flSubmit?.setOnClickListener {
            submit()
        }
        binding?.etText?.addTextChangedListener { text ->
            setSubmitEnabled(text != null && text.isNotEmpty() && text.isNotBlank())
        }
        setSubmitEnabled(false)
    }

    private fun submit() {
        val text = binding?.etText?.text?.toString() ?: ""
        lifecycleScope.launch {
            setSubmitEnabled(false)
            val sent = profileViewModel.sendContentSuggestion(text)
            setSubmitEnabled(true)
            if (sent) {
                Toast.makeText(
                    context,
                    R.string.your_issue_successfully_submitted,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showError(error: FeedbackError) {
        val errorText = when (error.errorType) {
            FeedbackErrorType.SPAMMING_ERROR -> {
                getString(R.string.please_wait_a_while_before_submit_again)
            }
            FeedbackErrorType.EMPTY_MESSAGE -> {
                getString(R.string.type_your_suggestion_before_submit)
            }
            FeedbackErrorType.NOT_AUTHORIZED -> {
                getString(R.string.please_authorise_first)
            }
            FeedbackErrorType.API_ERROR -> {
                (error.exception as? ApiErrorThrowable)?.let {
                    when (it.errorType) {
                        ApiError.NETWORK_PROBLEM -> {
                            getString(R.string.error_network_problem)
                        }
                        else -> {
                            getString(R.string.error_server_error)
                        }
                    }
                } ?: getString(R.string.unknown_error)
            }
        }
        Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
    }

    private fun setSubmitEnabled(isEnabled: Boolean) {
        binding?.flSubmit?.isEnabled = isEnabled
        binding?.flSubmit?.alpha = if (isEnabled) 1.0f else 0.5f
    }
}