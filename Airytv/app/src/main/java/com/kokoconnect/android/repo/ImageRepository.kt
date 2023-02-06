package com.kokoconnect.android.repo

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.database.getIntOrNull
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import com.kokoconnect.android.AiryTvApp
import com.kokoconnect.android.model.response.ApiError
import com.kokoconnect.android.model.response.ApiErrorThrowable
import com.kokoconnect.android.util.AppParams
import com.kokoconnect.android.util.BitmapUtils
import com.kokoconnect.android.util.NetworkUtils
import com.kokoconnect.android.util.Orientation
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject


class ImageRepository @Inject constructor(
    private val app: AiryTvApp
) {
    companion object {
        const val IMAGE_EXT_JPG = "jpg"
        const val IMAGE_MIME_TYPE_JPEG = "image/jpeg"
        const val DEFAULT_IMAGE_COMPRESS_QUALITY = 100
    }

    fun loadImageFromUrl(imageUrl: Uri?): Bitmap? {
        imageUrl ?: return null
        val orientation = try {
            getOrientation(imageUrl)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Orientation()
        }
        Timber.d("loadFromUrl() url ${imageUrl} orientation ${orientation.rotation}")
        val bitmap = try {
            BitmapFactory.Options().run {
                val padding = Rect(0, 0, 0, 0)
                openInputStreamFromUri(imageUrl)?.use {
                    BitmapFactory.decodeStream(it, padding, this)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        bitmap ?: return null
        return if (orientation.hasRotation()) {
            BitmapUtils.rotateBitmap(bitmap, orientation)
        } else {
            bitmap
        }
    }

    fun saveImageToUri(
        resultBitmap: Bitmap?,
        saveUri: Uri?,
        compressQuality: Int = DEFAULT_IMAGE_COMPRESS_QUALITY
    ): Boolean {
        if (resultBitmap == null || resultBitmap.isRecycled) {
            return false
        }
        return try {
            saveUri ?: return false
            var saved = false
            app.contentResolver?.openOutputStream(saveUri)?.use { os ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, os)
                os.flush()
                saved = true
            }
            saved
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveImageToFile(
        resultBitmap: Bitmap?,
        saveFile: File?,
        compressQuality: Int = DEFAULT_IMAGE_COMPRESS_QUALITY
    ): Boolean {
        if (resultBitmap == null || resultBitmap.isRecycled) {
            return false
        }
        return try {
            saveFile ?: return false
            var saved = false
            saveFile.outputStream().use { os ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, os)
                os.flush()
                saved = true
            }
            saved
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteImage(imageUri: Uri?) {
        val imagePath = imageUri?.path ?: return
        val file = File(imagePath)
        if (file.exists() && file.isFile) {
            try {
                file.delete()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else {
            try {
                val contentResolver: ContentResolver = app.contentResolver
                contentResolver.delete(imageUri, null, null)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteImage(imageFile: File?) {
        imageFile ?: return
        if (imageFile.exists() && imageFile.isFile) {
            try {
                imageFile.delete()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getOrientation(imageUri: Uri): Orientation {
        var orientation: Orientation = Orientation()
        try {
            var orientationExists = false
            app.contentResolver.query(
                imageUri,
                arrayOf<String>(MediaStore.Images.ImageColumns.ORIENTATION),
                null,
                null,
                null
            )?.use {
                if (it.count != 1) {
                    it.close()
                }
                it.moveToFirst()
                orientation.rotation = it.getIntOrNull(0)?.toFloat() ?: 0f
                orientationExists = true
                it.close()
            }
            if (orientationExists) {
                return orientation
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        try {
            val inputStream = app.contentResolver.openInputStream(imageUri) ?: return orientation
            ExifInterface(inputStream).apply {
                val exifOrientation = getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                ) ?: ExifInterface.ORIENTATION_UNDEFINED
                orientation = Orientation(exifOrientation)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return orientation
    }

    fun openInputStreamFromUri(fileUri: Uri): InputStream? {
        return try {
            app.contentResolver.openInputStream(fileUri)
        } catch (ex: Exception) {
            null
        }
    }

    fun getMimeType(uri: Uri?): String? {
        uri ?: return null
        return if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            app.contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            } else {
                null
            }
        }
    }

    fun getMimeType(file: File?): String? {
        file ?: return null
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
        return if (extension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else {
            null
        }

    }

    fun getExtension(mimeType: String): String {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.replace(".", "") ?: IMAGE_EXT_JPG
    }

    private fun getFilePath(uri: Uri?): String? {
        uri ?: return null
        val imageType = app.contentResolver.getType(uri).toString()
        val filePath = app.contentResolver.query(uri, null, null, null, null).use { cursor ->
            if (cursor == null) {
                uri.path
            } else {
                cursor.moveToFirst()
                val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                cursor.getString(index)
            }
        }
        Timber.d("getFilePath() uri = $uri imageType = $imageType filePath = $filePath")
        return filePath
    }

    private fun addImageToGallery(
        filepath: String,
        title: String? = null,
        mimeType: String? = null,
        description: String? = null
    ): Uri? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, title)
        values.put(MediaStore.Images.Media.DESCRIPTION, description)
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        values.put(MediaStore.MediaColumns.DATA, filepath)
        return app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun imageExists(imageUri: Uri?): Boolean {
        return if (imageUri != null) {
            try {
                app.contentResolver.openInputStream(imageUri)
                true
            } catch (e: java.lang.Exception) {
                Timber.d("imageExists() $imageUri NOT exists")
                false
            }
        } else {
            false
        }
    }

    private fun getTempImagesDir(): File? {
        val resultDir = app.externalCacheDir ?: return null
        if (!resultDir.exists()) {
            resultDir.mkdirs()
        }
        return resultDir
    }

    private fun getImagesDir(): File? {
        val resultDir = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
        if (!resultDir.exists()) {
            resultDir.mkdirs()
        }
        return resultDir
    }

    fun createTempImageOutputUri(name: String, mimeType: String): Uri? {
        val extension = getExtension(mimeType)
        val fileName = "${name}.${extension}"
        val resultFile = File(getTempImagesDir(), fileName)
        val uri = FileProvider.getUriForFile(app, AppParams.fileProviderAuthority, resultFile)
        return uri
    }

    fun createImageOutputUri(
        name: String,
        mimeType: String,
        description: String? = null
    ): Uri? {
        val resolver = app.contentResolver
        val uri = if (Build.VERSION.SDK_INT >= 29) {
            val relativePath = Environment.DIRECTORY_PICTURES
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.TITLE, name)
                put(MediaStore.Images.Media.DESCRIPTION, description)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?.replace(".", "") ?: IMAGE_EXT_JPG
            val fileName = "${name}.${extension}"
            val resultFile = File(getImagesDir(), fileName)
            addImageToGallery(resultFile.absolutePath, name, mimeType)
            FileProvider.getUriForFile(app, AppParams.fileProviderAuthority, resultFile)
        }
        return uri
    }

    fun createTempImageOutputFile(name: String, mimeType: String): File? {
        val dir = getTempImagesDir()
        val extension = getExtension(mimeType)
        val fileName = "${name}.${extension}"
        val file = File(dir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    fun createImageOutputFile(name: String, mimeType: String): File? {
        val dir = getImagesDir()
        val extension = getExtension(mimeType)
        val fileName = "${name}.${extension}"
        val file = File(dir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    fun copyImageUri(imageUri: Uri?, name: String, toTemp: Boolean = false): Uri? {
        imageUri ?: return null
        var resultFileUrl: Uri? = null
        try {
            val mimeType = getMimeType(imageUri) ?: IMAGE_MIME_TYPE_JPEG
            val imageInputStream = app.contentResolver.openInputStream(imageUri) ?: return null
            val outputUri = if (toTemp) {
                createTempImageOutputUri(name, mimeType) ?: return null
            } else {
                createImageOutputUri(name, mimeType) ?: return null
            }
            val imageOutputStream = app.contentResolver.openOutputStream(outputUri) ?: return null
            copyStreamToStream(imageInputStream, imageOutputStream)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return null
        }
        return resultFileUrl
    }

    fun copyImageFile(imageUri: Uri?, name: String, toTemp: Boolean = false): File? {
        imageUri ?: return null
        return try {
            val mimeType = getMimeType(imageUri) ?: IMAGE_MIME_TYPE_JPEG
            val imageInputStream = app.contentResolver.openInputStream(imageUri) ?: return null
            val outputFile = if (toTemp) {
                createTempImageOutputFile(name, mimeType) ?: return null
            } else {
                createImageOutputFile(name, mimeType) ?: return null
            }
            val imageOutputStream = outputFile.outputStream()
            copyStreamToStream(imageInputStream, imageOutputStream)
            outputFile
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun copyStreamToStream(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }


    private fun checkApiFailureThrowable(t: Throwable, errorLiveData: MutableLiveData<ApiError>) {
        val apiErrorThrowable = getApiErrorThrowable(t)
        errorLiveData.postValue(apiErrorThrowable.errorType)
    }

    private fun getApiErrorThrowable(t: Throwable): ApiErrorThrowable {
        return try {
            throw t
        } catch (ex: IOException) {
            if (NetworkUtils.isInternetAvailable()) {
                ApiErrorThrowable(ApiError.SERVER_ERROR)
            } else {
                ApiErrorThrowable(ApiError.NETWORK_PROBLEM)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ApiErrorThrowable(ApiError.SERVER_ERROR)
        }
    }
}