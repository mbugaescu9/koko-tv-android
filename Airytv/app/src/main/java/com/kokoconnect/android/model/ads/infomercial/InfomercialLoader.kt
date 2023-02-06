package com.kokoconnect.android.model.ads.infomercial

import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.lang.Exception
import java.net.URL
import kotlin.coroutines.CoroutineContext

class InfomercialLoader : CoroutineScope {
    companion object {
        private const val EOF = -1
        private const val BUFFER_SIZE = 1024
    }
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    private var sourceUrl: URL? = null
    var loaded = false
    private set
    var infomercial: File? = null
    private var eventsListener: InfomercialEventsListener? = null

    private var contentLength = 0L
    private var downloadedLength = 0L

    fun setEventsListener(listener: InfomercialEventsListener?) {
        eventsListener = listener
    }

    fun load(sourceUrl: URL) {
        release()
        var destinationFile: File? = null
        launch {
            try {
                destinationFile = InfomercialManager.newCacheFile()
                Timber.d("load() to file ${destinationFile?.absolutePath}")
                this@InfomercialLoader.sourceUrl = sourceUrl

                val connection = sourceUrl.openConnection()
                connection.connect()

                eventsListener?.onInfomercialEvent(InfomercialEvent.LOAD_STARTED, this@InfomercialLoader)
                contentLength = connection.contentLength.toLong()
                val inputStream = BufferedInputStream(connection.getInputStream())
                val outputStream = FileOutputStream(destinationFile)
                download(inputStream, outputStream)
                inputStream.close()
                outputStream.close()
                if (downloadedLength >= contentLength) {
                    loaded = true
                    infomercial = destinationFile
                    eventsListener?.onInfomercialEvent(InfomercialEvent.LOADED, this@InfomercialLoader)
                }
                Timber.d("load() loaded = ${loaded} to file ${destinationFile?.absolutePath}")
            } catch(ex: Exception) {
                eventsListener?.onInfomercialEvent(InfomercialEvent.LOAD_FAILED, this@InfomercialLoader)
                Timber.e("load() error")
                ex.printStackTrace()
            }
        }
    }

    @Throws(java.io.IOException::class)
    private fun download(inputStream: InputStream, outputStream: OutputStream) {
        downloadedLength = 0
        val buffer = ByteArray(BUFFER_SIZE)
        var byteCount = inputStream.read(buffer)
        while(byteCount != EOF) {
            downloadedLength += byteCount
            outputStream.write(buffer, 0, byteCount)
//            Timber.d("download() downloaded ${(downloadedLength.toDouble() / contentLength) * 100.0} \\% from ${sourceUrl}")
            byteCount = inputStream.read(buffer)
        }
        outputStream.flush()
    }


    fun release() {
        Timber.d("release()")
        try {
            coroutineContext.cancelChildren()
            loaded = false
            infomercial?.delete()
            infomercial = null
            sourceUrl = null
            contentLength = 0
            downloadedLength = 0
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

}