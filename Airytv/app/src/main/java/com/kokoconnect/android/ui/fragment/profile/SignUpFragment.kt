package com.kokoconnect.android.ui.fragment.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.FragmentSignUpBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.auth.SignUpRequest
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AuthError
import com.kokoconnect.android.util.ActivityUtils
import com.kokoconnect.android.vm.profile.AuthInterfaceViewModel
import com.kokoconnect.android.vm.profile.SignUpViewModel
import timber.log.Timber
import javax.inject.Inject

class SignUpFragment : Fragment(), Injectable {
    @Inject
    lateinit var serverAuthRepositoryApi: AuthRepository
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    val signUpViewModel: SignUpViewModel by activityViewModels()
    val authInterfaceViewModel: AuthInterfaceViewModel by activityViewModels { viewModelFactory }

    private var email = ""
    private var firstName = ""
    private var lastName = ""
    private var password = ""
    private var repeatPassword = ""
    private var binding: FragmentSignUpBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        signUpViewModel.loginLiveData.observe(viewLifecycleOwner, Observer {
            binding?.emailEditText?.setText(it)
        })
        signUpViewModel.passwordLiveData.observe(viewLifecycleOwner, Observer {
            binding?.passwordEditText?.setText(it)
        })
        signUpViewModel.repeatPasswordLiveData.observe(viewLifecycleOwner, Observer {
            binding?.repeatPasswordEditText?.setText(it)
        })

        signUpViewModel.resultLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                requestSended(false)
                Toast.makeText(context, R.string.signup_success, Toast.LENGTH_LONG).show()
                Timber.d("Successfully sign up, token = ${it.token}")
                ActivityUtils.hideKeyboard(activity)
                findNavController().popBackStack()
                signUpViewModel.resultLiveData.value = null
            }
        })
        signUpViewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                requestSended(false)
                Timber.d("Sign up errors, code = ${it.code}, message = ${it.message}")
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
//                    .setTitle(getString(R.string.signup_error_title))
//                    .setMessage(message)
//                    .build(ctx)
//                    .show()
                signUpViewModel.errorLiveData.value = null
            }
        })

    }

    private fun setupViews() {
        binding?.emailEditText?.doOnTextChanged { text, start, count, after ->
            email = text.toString()
        }
        binding?.firstNameEditText?.doOnTextChanged { text, start, count, after ->
            firstName = text.toString()
        }
        binding?.lastNameEditText?.doOnTextChanged { text, start, count, after ->
            lastName = text.toString()
        }
        binding?.passwordEditText?.doOnTextChanged { text, start, count, after ->
            password = text.toString()
        }
        binding?.repeatPasswordEditText?.doOnTextChanged { text, start, count, after ->
            repeatPassword = text.toString()
        }
        binding?.signUpButton?.setOnClickListener{
            signUp()
        }
    }

    override fun onPause() {
        super.onPause()
        signUpViewModel.loginLiveData.postValue(email)
        signUpViewModel.firstNameLiveData.postValue(firstName)
        signUpViewModel.lastNameLiveData.postValue(lastName)
        signUpViewModel.passwordLiveData.postValue(password)
        signUpViewModel.repeatPasswordLiveData.postValue(repeatPassword)
    }

    private fun isFieldsValid(): Boolean {
        email = email.replaceSpaces()
        binding ?: return false
        binding?.emailEditText?.setText(email)
        return when{
            email.isEmpty() -> {
                binding?.emailEditText?.setCustomError(getString(R.string.signup_error_email))
                false
            }
            !isValidEmail(email) -> {
                binding?.emailEditText?.setCustomError(getString(R.string.signup_error_email))
                false
            }
            password.isEmpty() -> {
                binding?.passwordEditText?.setCustomError(getString(R.string.signup_error_password))
                false
            }
            password != repeatPassword -> {
                binding?.repeatPasswordEditText?.setCustomError(getString(R.string.signup_error_repeat_password))
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun signUp(){
        if (isFieldsValid()) {
            serverAuthRepositoryApi.signUp(
                SignUpRequest(email, firstName, lastName, password, repeatPassword),
                signUpViewModel.resultLiveData,
                signUpViewModel.errorLiveData
            )
            requestSended(true)
        }
    }

    private fun requestSended(isSended: Boolean){
        binding?.signUpButton?.isEnabled = !isSended
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
