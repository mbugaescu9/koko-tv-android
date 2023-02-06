package com.kokoconnect.android.model.ads.infomercial

import com.kokoconnect.android.AiryTvApp
import java.io.File
import java.io.IOException
import java.util.*

object InfomercialManager {
    const val INFOMERCIALS_FOLDER_NAME = "airyinfomercials"

    val cacheDirectory = File(AiryTvApp.instance.externalCacheDir,
        INFOMERCIALS_FOLDER_NAME
    )

    fun init() {
        clearCache()
    }

    fun clearCache() {
        try {
            cacheDirectory.deleteRecursively()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun newCacheFile() : File? {
        var file: File? = null
        try {
            file = File(cacheDirectory, UUID.randomUUID().toString())
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdir()
            }
            file.createNewFile()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return file
    }
}