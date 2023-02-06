package com.kokoconnect.android.ui.fragment.profile

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.GiveawaysAdapter
import com.kokoconnect.android.databinding.FragmentProfileBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.giveaways.GiveawaysItem
import com.kokoconnect.android.model.profile.Profile
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.ui.dialog.PickImageDialog
import com.kokoconnect.android.ui.fragment.BaseFragment
import com.kokoconnect.android.util.DeviceUtils
import com.kokoconnect.android.util.IntentUtils
import com.kokoconnect.android.util.PermissionUtils
import com.kokoconnect.android.util.navigateSafe
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.profile.ProfileViewModel
import com.kokoconnect.android.vm.profile.AuthViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProfileFragment : BaseFragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.PROFILE.defaultName
            this.type = ScreenType.PROFILE
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    var binding: FragmentProfileBinding? = null
    private val authViewModel: AuthViewModel by activityViewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by activityViewModels { viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }
    private lateinit var adapter: GiveawaysAdapter

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result?.data?.data?.let { imageUri ->
                lifecycleScope.launch {
                    binding?.lockUi?.root?.isVisible = true
                    profileViewModel.uploadImage(imageUri)
                    binding?.lockUi?.root?.isVisible = false
                }
            }
        }

    private val galleryPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (PermissionUtils.isPermissionsGranted(it)) {
                openGallery()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val cameraImageUri = profileViewModel.cameraImageUri
            if (result.resultCode == Activity.RESULT_OK && cameraImageUri != null) {
                lifecycleScope.launch {
                    binding?.lockUi?.root?.isVisible = true
                    profileViewModel.uploadImage(
                        cameraImageUri
                    )
                    binding?.lockUi?.root?.isVisible = false
                    profileViewModel.cameraImageUri = null
                }
            }
        }

    private val cameraPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (PermissionUtils.isPermissionsGranted(it)) {
                openCamera()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        authViewModel.isAuthorized.observe(viewLifecycleOwner, Observer { isAuthorized ->
            if (isAuthorized != null) {
                binding?.tvSignIn?.isVisible = !isAuthorized
                binding?.tvSignOut?.isVisible = isAuthorized
                binding?.llPointHistory?.isVisible = isAuthorized
                binding?.llSuggestionFeedback?.isVisible = isAuthorized
            }
        })
        profileViewModel.profileLiveData.observe(viewLifecycleOwner, Observer { profile ->
            showProfile(profile)
        })
        profileViewModel.giveawaysOwnedLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                showGiveaways(it)
            }
        })
    }

    private fun setupViews() {
        adapter = GiveawaysAdapter(listener = null)
        binding?.rvGifts?.adapter = adapter
        binding?.rvGifts?.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        binding?.llChangeTheme?.setOnClickListener {
            openThemeDialog()
        }
        binding?.llPrivacyPolicy?.setOnClickListener {
            findNavController().navigateSafe(R.id.action_fragmentProfile_to_fragmentPrivacyPolicy)
        }
        binding?.llSuggestionFeedback?.setOnClickListener {
            findNavController().navigateSafe(R.id.action_fragmentProfile_to_fragmentSuggestionFeedbackSelect)
        }
        binding?.llPointHistory?.setOnClickListener {
            findNavController().navigateSafe(R.id.action_fragmentProfile_to_fragmentTransactions)
        }
        binding?.tvSignIn?.setOnClickListener {
            findNavController().navigateSafe(R.id.action_fragmentProfile_to_fragmentSignIn)
        }
        binding?.tvSignOut?.setOnClickListener {
            authViewModel.signOut()
        }
        binding?.ivAvatar?.setOnClickListener {
            changeAvatar()
        }
        binding?.ivAvatarPlaceholder?.setOnClickListener {
            changeAvatar()
        }
    }

    private fun showProfile(profile: Profile?) {
        val context = context ?: return


        profile?.username?.let {
            binding?.tvUserName?.setText(getString(R.string.user_profile_hi, it))
        } ?: binding?.tvUserName?.setText("")

        val avatarUrl = profile?.avatar
        val ivAvatar = binding?.ivAvatar
        if (avatarUrl != null && ivAvatar != null) {
            ivAvatar.isVisible = true
            Glide.with(context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_profile_avatar_placeholder)
                .addListener(object: RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.ivAvatarPlaceholder?.isVisible = true
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.ivAvatarPlaceholder?.isVisible = false
                        return false
                    }
                })
                .into(ivAvatar)
        } else {
            binding?.ivAvatar?.isVisible = false
            binding?.ivAvatarPlaceholder?.isVisible = true
        }
    }

    private fun changeAvatar() {
        if (authViewModel.isAuthorized()) {
            pickImage()
        } else {
            Toast.makeText(context, R.string.please_authorise_first, Toast.LENGTH_SHORT).show()
            authViewModel.openAuth()
        }
    }

    private fun pickImage() {
        val pickImageDialog = PickImageDialog()
        pickImageDialog.listener = object : PickImageDialog.Listener {
            override fun pickImage(imageSource: PickImageDialog.ImageSource) {
                when (imageSource) {
                    PickImageDialog.ImageSource.GALLERY -> {
                        openGallery()
                    }
                    PickImageDialog.ImageSource.CAMERA -> {
                        openCamera()
                    }
                }
            }
        }
        pickImageDialog.show(parentFragmentManager, PickImageDialog.TAG)
    }

    private fun showGiveaways(giveaways: List<GiveawaysItem>) {
        binding?.tvCollectYourGift?.isVisible = giveaways.isNotEmpty()
        binding?.rvGifts?.isVisible = giveaways.isNotEmpty()
        adapter.items = giveaways
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen, this)
        lifecycleScope.launch {
            profileViewModel.requestProfile()
        }
    }

    private fun openCamera() {
        val activity = activity ?: return
        val permissions = PermissionUtils.getCameraPermissions()
        if (DeviceUtils.getCameraExists(activity)) {
            if (PermissionUtils.isPermissionsGranted(activity, permissions)) {
                val takePictureIntent =
                    IntentUtils.getCameraIntent(profileViewModel.createCameraImageUri())
                if (IntentUtils.hasAppForIntent(activity, takePictureIntent)) {
                    cameraLauncher.launch(takePictureIntent)
                } else {
                    Toast.makeText(context, R.string.camera_app_not_found, Toast.LENGTH_LONG).show()
                }
            } else {
                PermissionUtils.requestPermissions(cameraPermissionsLauncher, permissions)
            }
        } else {
            Toast.makeText(context, R.string.camera_not_found, Toast.LENGTH_LONG).show()
        }
    }

    private fun openGallery() {
        val activity = activity ?: return
        val permissions = PermissionUtils.getGalleryPermissions()
        if (PermissionUtils.isPermissionsGranted(activity, permissions)) {
            val photoPickerIntent = IntentUtils.getGalleryIntent()
            if (IntentUtils.hasAppForIntent(activity, photoPickerIntent)) {
                galleryLauncher.launch(photoPickerIntent)
            } else {
                Toast.makeText(context, R.string.gallery_app_not_found, Toast.LENGTH_LONG).show()
            }
        } else {
            PermissionUtils.requestPermissions(
                galleryPermissionsLauncher,
                PermissionUtils.READ_WRITE_PERMISSIONS
            )
        }
    }
}