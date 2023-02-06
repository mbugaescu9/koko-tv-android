package com.kokoconnect.android.ui.fragment.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.databinding.FragmentSignInBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.model.auth.TokenResponse
import com.kokoconnect.android.model.auth.User
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.AuthError
import com.kokoconnect.android.ui.dialog.CustomDialog
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.profile.AuthViewModel
import com.facebook.*
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import com.kokoconnect.android.R

class SignInFragment : Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.AUTH.defaultName
            this.type = ScreenType.AUTH
        }
    }

    @Inject
    lateinit var serverAuthRepositoryApi: AuthRepository
    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val facebookAuthCallback = CallbackManager.Factory.create()

    val authViewModel: AuthViewModel by activityViewModels{ viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels{ viewModelFactory }

    lateinit var googleApiClient: GoogleSignInClient
    private val resultLiveData = MutableLiveData<TokenResponse?>()
    private val errorLiveData = MutableLiveData<AuthError?>()
    private var binding: FragmentSignInBinding? = null

    private val signInGoogleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result?.data)
            if (task != null) {
                val account = task.getResult(ApiException::class.java)
                authWithGoogleToken(account)
            } else {
                errorMessage("Google SignIn Task is null")
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.NETWORK_ERROR -> errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                else -> {
                    if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                        errorMessage("Google SignIn problem: ${e.statusCode}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private var email: String? = null
    private var token: String? = null
    private var user: User? = null

    enum class ActivityResult(val type: String) {
        GOOGLE("google"), FACEBOOK("facebook")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        googleApiClient = GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(AppParams.googleWebClientId)
                .requestEmail()
                .build()
        )
        LoginManager.getInstance().registerCallback(facebookAuthCallback,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    val facebookToken = loginResult.accessToken
                    authWithFacebookToken(facebookToken)
                }

                override fun onCancel() {}

                override fun onError(e: FacebookException) {
                    if (e.message.equals("net::ERR_INTERNET_DISCONNECTED")) {
                        errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                    } else {
                        Timber.d("facebook onError() ${e?.printStackTrace()}")
                        errorMessage("Facebook SignIn problem: ${e.message}")
                        e.printStackTrace()
                    }
                }
            })

        binding?.llSignInEmail?.setOnClickListener {
            clearData()
            findNavController().navigateSafe(R.id.action_fragmentSignIn_to_fragmentSignInEmail)
        }

        binding?.llSignInGoogle?.setOnClickListener {
            clearData()
            val signInIntent = googleApiClient.signInIntent
            signInGoogleLauncher.launch(signInIntent)
        }

        binding?.llSignInFacebook?.setOnClickListener {
            clearData()
            val loginManager = LoginManager.getInstance()
            loginManager.loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK
            loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile", "user_friends"))
        }

        resultLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                onAuthorized(it)
                resultLiveData.value = null
            }
        })

        errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Timber.d("Sign up errors, code = ${it.code}, message = ${it.message}")
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
                errorMessage(message)
                errorLiveData.value = null
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ActivityResult.GOOGLE.ordinal -> {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    Timber.d("onActivityResult() GOOGLE ${task}")
                    if (task != null) {
                        val account = task.getResult(ApiException::class.java)
                        authWithGoogleToken(account)
                    } else {
                        errorMessage("Google SignIn Task is null")
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.NETWORK_ERROR -> errorLiveData.postValue(AuthError.NETWORK_PROBLEM)
                        else -> {
                            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                                errorMessage("Google SignIn problem: ${e.statusCode}")
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
        Timber.d("onActivityResult() fragment")
        facebookAuthCallback.onActivityResult(requestCode, resultCode, data)
    }

    private fun authWithGoogleToken(googleSignInAccount: GoogleSignInAccount?) {
        val idToken = googleSignInAccount?.idToken ?: ""
        email = googleSignInAccount?.email
        Timber.d("authWithGoogleToken() GOOGLE ${idToken}")
        if (idToken != "") {
            serverAuthRepositoryApi.social(
                ActivityResult.GOOGLE.type,
                idToken,
                resultLiveData,
                errorLiveData
            )
        } else {
            errorMessage("Google SignIn token is empty")
        }
    }

    private fun authWithFacebookToken(facebookToken: AccessToken?) {
        if (facebookToken != null) {
            val emailRequest =
                GraphRequest.newMeRequest(facebookToken) {
                        jsonObject: JSONObject?, response: GraphResponse? ->
                    try {
                        email = jsonObject?.getString("email")
                    } catch(ex: JSONException) {
                        // No value for email
                        ex.printStackTrace()
                    }
                    val code = facebookToken.token
                    val userId = facebookToken.userId

                    Timber.d("authWithFacebookToken() FACEBOOK code ${code} userId ${userId}")
                    if (code != null && userId != null) {
                        serverAuthRepositoryApi.social(
                            ActivityResult.FACEBOOK.type,
                            code,
                            resultLiveData,
                            errorLiveData
                        )
                    } else {
                        errorMessage("Facebook SignIn token is null")
                    }
                }
            emailRequest.parameters = Bundle().apply{
                putString("fields", "email")
            }
            emailRequest.executeAsync()
        } else {
            errorMessage("Facebook SignIn token is null")
        }
    }

    private fun onAuthorized(tokenResponse: TokenResponse){
        token = tokenResponse.token
        user = tokenResponse.user
        Timber.d("onAuthorized()")
//        if (email == null) {
//            val ctx = context ?: return
//            CustomDialog.Builder()
//                .setInputEnabled(true)
//                .setInputHint(getString(R.string.signin_hint_email))
//                .setMessage(getString(R.string.signin_request_email))
//                .setOnClickListener {
//                    email = it
//                    completeSignIn()
//                }
//                .build(ctx)
//                .show()
//        } else {
            completeSignIn()
//        }
    }

    private fun completeSignIn() {
        when {
            isValidToken(token) && isValidEmail(email) -> {
                Timber.d("Successfully sign in, token = ${token}, email = ${email}")
                Toast.makeText(context, R.string.signin_success, Toast.LENGTH_LONG).show()
                authViewModel.onSignedIn(user, token, email)
                findNavController().popBackStack(R.id.fragmentProfile, false)
            }
            !isValidEmail(email) -> {
                errorMessage(getString(R.string.signin_error_email))
            }
            else -> {
                errorMessage(getString(R.string.signin_error_server))
            }
        }
    }

    private fun errorMessage(message: String) {
        Toast.makeText(context, "failed", Toast.LENGTH_LONG).show();
//        CustomDialog.Builder()
//            .setTitle(getString(R.string.signin_error_title))
//            .setMessage(message)
//            .build(context ?: return)
//            .show()
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun isValidToken(target: CharSequence?): Boolean {
        return target != null && target.isNotEmpty()
    }

    private fun clearData() {
        token = null
        email = null
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen, this)
    }

}