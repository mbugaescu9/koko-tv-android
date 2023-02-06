package com.kokoconnect.android.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentSignInEmailBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.auth.SignInRequest
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AuthError
import com.kokoconnect.android.ui.dialog.CustomDialog
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.profile.AuthViewModel
import com.kokoconnect.android.vm.profile.SignInViewModel
import timber.log.Timber
import javax.inject.Inject

class SignInEmailFragment : Fragment(), Injectable {
    @Inject
    lateinit var serverAuthRepositoryApi: AuthRepository
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val signInViewModel: SignInViewModel by activityViewModels()
    val authViewModel: AuthViewModel by activityViewModels{ viewModelFactory }

    private var email = ""
    private var password = ""
    private var binding: FragmentSignInEmailBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInEmailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        setupViews()

        signInViewModel.loginLiveData.observe(viewLifecycleOwner, Observer {
            binding?.emailEditText?.setText(it)
        })
        signInViewModel.passwordLiveData.observe(viewLifecycleOwner, Observer {
            binding?.passwordEditText?.setText(it)
        })
        signInViewModel.resultLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                requestSended(false)
                val ctx = context ?: return@Observer
                Timber.d("Successfully sign in, email = $email, token = ${it.token}")
                it.token?.let { token ->
                    authViewModel.onSignedIn(it.user, token, email)
                    ActivityUtils.hideKeyboard(activity)
                    findNavController().popBackStack(R.id.fragmentProfile, false)
                }
                signInViewModel.resultLiveData.value = null
            }
        })
        signInViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                requestSended(false)
                Timber.d("Sign in errors, code = ${it.code}, message = ${it.message}")
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
//                CustomDialog.Builder()
//                    .setTitle(getString(R.string.signin_error_title))
//                    .setMessage(message)
//                    .build(ctx)
//                    .show()
                Toast.makeText(context, R.string.signin_error_title, Toast.LENGTH_LONG).show()

                signInViewModel.errorLiveData.value = null
            }
        })
    }

    private fun setupViews() {
        val activity = requireActivity()

        binding?.emailEditText?.doOnTextChanged { text, start, count, after ->
            email = text.toString()
        }
        binding?.passwordEditText?.doOnTextChanged { text, start, count, after ->
            password = text.toString()
        }

        binding?.signInButton?.setOnClickListener {
            signIn()
        }
        binding?.signUpTextView?.setOnClickListener {
            goSignUp()
        }
        binding?.forgotPasswordTextView?.setOnClickListener {
            goResetPassword()
        }
    }

    override fun onPause() {
        super.onPause()
        signInViewModel.loginLiveData.postValue(email)
        signInViewModel.passwordLiveData.postValue(password)
    }

    private fun isFieldsValid(): Boolean {
        email = email.replaceSpaces()
        binding?.emailEditText?.setText(email)
        return when {
            email.isEmpty() -> {
                binding?.emailEditText?.setCustomError(getString(R.string.signin_error_email))
                false
            }
            !isValidEmail(email) -> {
                binding?.emailEditText?.setCustomError(getString(R.string.signin_error_email))
                false
            }
            password.isEmpty() -> {
                binding?.passwordEditText?.setCustomError(getString(R.string.signin_error_password))
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun signIn() {
        if (isFieldsValid()) {
            serverAuthRepositoryApi.signIn(
                SignInRequest(email, password),
                signInViewModel.resultLiveData,
                signInViewModel.errorLiveData
            )
            requestSended(true)
        }
    }

    private fun requestSended(isSended: Boolean) {
        binding?.signInButton?.isEnabled = !isSended
    }

    private fun goSignUp() {
        findNavController().navigateSafe(R.id.action_fragmentSignInEmail_to_fragmentSignUp)
    }

    private fun goResetPassword() {
        findNavController().navigateSafe(R.id.action_fragmentSignInEmail_to_fragmentResetPassword)
    }


    private fun EditText.setCustomError(
        message: String,
        clearText: Boolean = true,
        requestFocus: Boolean = true
    ) {
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

fun Fragment.getThemeColor(resId: Int): Int {
    return ActivityUtils.getColorFromAttr(requireContext(), resId)
}