package com.kokoconnect.android.vm.profile

import android.net.Uri
import androidx.lifecycle.*
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.error.FeedbackError
import com.kokoconnect.android.model.error.FeedbackErrorType
import com.kokoconnect.android.model.giveaways.GiveawaysItem
import com.kokoconnect.android.model.profile.Profile
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.model.response.ProfileResponse
import com.kokoconnect.android.repo.AiryRepository
import com.kokoconnect.android.repo.AuthRepository
import com.kokoconnect.android.repo.ImageRepository
import com.kokoconnect.android.util.BitmapUtils
import com.kokoconnect.android.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val app: AiryTvApp,
    private val airyRepo: AiryRepository,
    private val authRepo: AuthRepository,
    private val imageRepo: ImageRepository
) : ViewModel() {
    companion object {
        const val FEEDBACK_SEND_PERIOD_MS = 10000L
    }
    var profileLiveData = MutableLiveData<Profile?>(null)
    var giveawaysOwnedLiveData: LiveData<List<GiveawaysItem>?> = profileLiveData.map {
        val giveaways = it?.gifts
        giveaways?.forEach {
            it.isActive = false
            it.isCardCodeVisible = true
        }
        giveaways ?: emptyList()
    }
    var cameraImageUri: Uri? = null
    private var lastFeedbackSentTimeMs: Long = 0L
    var feedbackError = MutableLiveData<FeedbackError?>()

    fun getProfile(): Profile? {
        return profileLiveData.value
    }

    fun setProfile(profile: Profile?) {
        this.profileLiveData.postValue(profile)
    }

    suspend fun requestProfile() {
        val token = authRepo.getToken()
        if (token == null) {
            return
        }
        var profileResponse: ProfileResponse? = null
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                profileResponse = airyRepo.getProfile(token)
            } catch (ex: ApiErrorThrowable) {
                null
            }
        }
        profileResponse?.let {
            setProfile(Profile(it))
        }
    }


    fun createCameraImageUri(): Uri? {
        val name = UUID.randomUUID().toString()
        val mimeType = ImageRepository.IMAGE_MIME_TYPE_JPEG
        cameraImageUri = imageRepo.createTempImageOutputUri(name, mimeType)
        return cameraImageUri
    }

    suspend fun uploadImage(
        imageUri: Uri,
        maxSize: Int = 1080,
        compressionQuality: Int = 50
    ) {
        val token = authRepo.getToken()
        if (token == null) {
            return
        }
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            if (imageRepo.imageExists(imageUri)) {
                // load raw image data and rotate it, then save rotated data into another file
                val imageBitmap = imageRepo.loadImageFromUrl(imageUri)
                Timber.d("uploadImage() image loaded bitmap = ${imageBitmap}")
                val mimeType = ImageRepository.IMAGE_MIME_TYPE_JPEG
                val name = UUID.randomUUID().toString()
                val imageFile = imageRepo.createTempImageOutputFile(name, mimeType)
                val scaledImageBitmap = BitmapUtils.scaleImage(imageBitmap, maxSize)
                Timber.d("uploadImage() image scaled bitmap = ${scaledImageBitmap}")
                imageBitmap?.recycle()
                val saved = imageRepo.saveImageToFile(
                    scaledImageBitmap,
                    imageFile,
                    compressionQuality
                )
                scaledImageBitmap?.recycle()
                Timber.d("uploadImage() image saved ${saved} into ${imageFile?.absolutePath}")
                if (saved && imageFile != null) {
                    val result = try {
                        airyRepo.uploadImage(token, imageFile, mimeType)
                    } catch (ex: ApiErrorThrowable) {
                        null
                    }
                    withContext(Dispatchers.Main) {
                        result?.let {
                            setProfile(Profile(it))
                        }
                    }
                }
                imageRepo.deleteImage(imageFile)
                Timber.d("uploadImage() image deleted")
            }
        }
    }

    private fun setFeedbackError(error: FeedbackError) {
        feedbackError.postValue(error)
    }

    private fun isFeedbackTextValid(text: String?): Boolean {
        return text != null && text.isNotEmpty() && text.isNotBlank()
    }

    private fun isFeedbackSendEnabled(): Boolean {
        return (DateUtils.getCurrentTime() - lastFeedbackSentTimeMs) > FEEDBACK_SEND_PERIOD_MS
    }

    suspend fun sendFeedback(text: String?): Boolean {
        val token = authRepo.getToken()
        if (token == null) {
            setFeedbackError(FeedbackError(FeedbackErrorType.NOT_AUTHORIZED))
            return false
        }
        if (!isFeedbackTextValid(text)) {
            setFeedbackError(FeedbackError(FeedbackErrorType.EMPTY_MESSAGE))
            return false
        }
        if (!isFeedbackSendEnabled()) {
            setFeedbackError(FeedbackError(FeedbackErrorType.SPAMMING_ERROR))
            return false
        }
        return try {
            val result = airyRepo.sendFeedback(token, text ?: "")
            lastFeedbackSentTimeMs = DateUtils.getCurrentTime()
            result
        } catch (ex: ApiErrorThrowable) {
            setFeedbackError(FeedbackError(FeedbackErrorType.API_ERROR, ex))
            false
        }
    }

    suspend fun sendContentSuggestion(text: String?): Boolean {
        val token = authRepo.getToken()
        if (token == null) {
            setFeedbackError(FeedbackError(FeedbackErrorType.NOT_AUTHORIZED))
            return false
        }
        if (!isFeedbackTextValid(text)) {
            setFeedbackError(FeedbackError(FeedbackErrorType.EMPTY_MESSAGE))
            return false
        }
        if (!isFeedbackSendEnabled()) {
            setFeedbackError(FeedbackError(FeedbackErrorType.SPAMMING_ERROR))
            return false
        }
        return try {
            val result = airyRepo.sendContentSuggestion(token, text ?: "")
            lastFeedbackSentTimeMs = DateUtils.getCurrentTime()
            result
        } catch (ex: ApiErrorThrowable) {
            setFeedbackError(FeedbackError(FeedbackErrorType.API_ERROR, ex))
            false
        }
    }
}