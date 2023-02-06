package com.kokoconnect.android.ui.fragment.profile


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentResetPasswordBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.auth.ResetPasswordRequest
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AuthError
import com.kokoconnect.android.ui.dialog.CustomDialog
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.vm.profile.ResetPasswordViewModel
import timber.log.Timber
import javax.inject.Inject

class ResetPasswordFragment : Fragment(), Injectable {
    @Inject
    lateinit var serverAuthRepositoryApi: AuthRepository
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val resetPasswordViewModel: ResetPasswordViewModel by activityViewModels()

    private var email = ""
    private var binding: FragmentResetPasswordBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        resetPasswordViewModel.loginLiveData.observe(viewLifecycleOwner, Observer {
            binding?.emailEditText?.setText(it)
        })
        resetPasswordViewModel.resultLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                requestSended(false)
                val ctx = context ?: return@Observer
                ActivityUtils.hideKeyboard(activity)
                CustomDialog.Builder()
                    .setTitle(getString(R.string.reset_password_success))
                    .setMessage(it.message)
                    .setOnClickListener {
                        findNavController().popBackStack()
                    }
                    .build(ctx)
                    .show()
                resetPasswordViewModel.resultLiveData.value = null
            }
        })
        resetPasswordViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                requestSended(false)
                Timber.d("Reset password errors, code = ${it.code}")
                val ctx = context ?: return@Observer
                val message = if (it.message.isNotEmpty()) {
                    it.message
                } else {
                    when (it) {
                        AuthError.NETWORK_PROBLEM -> "Network problem"
                        AuthError.SERVER_ERROR -> "Server errors"
                        AuthError.SERVER_RESULT_NOT_200 -> "Wrong credentials"
                        else -> it.code.toString()
                    }
                }
                CustomDialog.Builder()
                    .setTitle(getString(R.string.reset_password_error_title))
                    .setMessage(message)
                    .build(ctx)
                    .show()
                resetPasswordViewModel.errorLiveData.value = null
            }
        })
    }

    private fun setupViews() {
        binding?.emailEditText?.doOnTextChanged { text, start, count, after ->
            email = text.toString()
        }
        binding?.resetPasswordButton?.setOnClickListener {
            resetPassword()
        }
    }

    override fun onPause() {
        super.onPause()
        resetPasswordViewModel.loginLiveData.postValue(email)
    }

    private fun isFieldsValid(): Boolean {
        email = email.replaceSpaces()
        binding?.emailEditText?.setText(email)
        return when{
            email.isEmpty() -> {
                binding?.emailEditText?.setCustomError(getString(R.string.reset_password_error_email))
                false
            }
            !isValidEmail(email) -> {
                binding?.emailEditText?.setCustomError(getString(R.string.reset_password_error_email))
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun resetPassword(){
        if (isFieldsValid()) {
            serverAuthRepositoryApi.resetPassword(
                ResetPasswordRequest(email),
                resetPasswordViewModel.resultLiveData,
                resetPasswordViewModel.errorLiveData
            )
            requestSended(true)
        }
    }

    private fun requestSended(isSended: Boolean){
        binding?.resetPasswordButton?.isEnabled = !isSended
    }


    private fun EditText.setCustomError(message: String, clearText: Boolean = true, requestFocus: Boolean = true) {
        if (clearText) {
            this.setText("")
        }
        this.setHint(message)
        this.setHintTextColor(getThemeColor(R.attr.colorError))
        if (requestFocus) {
            this.requestFocus()
        }
    }

    private fun String.replaceSpaces(): String {
        return this.replace(" ", "")
    }
}
