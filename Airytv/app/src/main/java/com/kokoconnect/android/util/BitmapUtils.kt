package com.kokoconnect.android.util

import android.graphics.*
import android.graphics.Bitmap.*
import android.os.Build
import com.kokoconnect.android.AiryTvApp
import kotlin.math.roundToInt

object BitmapUtils {
    val app = AiryTvApp.instance
    private var matrix = Matrix()

    fun createThumbnail(originalBitmap: Bitmap, thumbnailMaxSize: Int): Bitmap? {
        var thumbnailWidth = 0
        var thumbnailHeight = 0
        if (originalBitmap.height > originalBitmap.width) {
            thumbnailHeight = thumbnailMaxSize
            thumbnailWidth =
                (originalBitmap.width.toFloat() / originalBitmap.height.toFloat() * thumbnailHeight).roundToInt()
        } else {
            thumbnailWidth = thumbnailMaxSize
            thumbnailHeight =
                (originalBitmap.height.toFloat() / originalBitmap.width.toFloat() * thumbnailWidth).roundToInt()
        }
        return createScaledBitmap(originalBitmap, thumbnailWidth, thumbnailHeight, false)
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Orientation): Bitmap? {
        val matrix = Matrix()
        matrix.setRotate(orientation.rotation)
        if (orientation.flipHorizontal) {
            matrix.postScale(-1f, 1f)
        } else if (orientation.flipHorizontal) {
            matrix.postScale(1f, -1f)
        } else if (orientation.flipHorizontal && orientation.flipVertical) {
            matrix.postScale(-1f, -1f)
        }
        return try {
            val bmRotated = createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        }
    }

    fun scaleImage(inputBitmap: Bitmap?, maxSize: Int): Bitmap? {
        inputBitmap ?: return null
        val outWidth: Int
        val outHeight: Int
        val inWidth = inputBitmap.width
        val inHeight = inputBitmap.height
        if (inWidth > inHeight) {
            outWidth = maxSize
            outHeight = inHeight * maxSize / inWidth
        } else {
            outHeight = maxSize
            outWidth = inWidth * maxSize / inHeight
        }
        return try {
            val scaledBitmap = createBitmap(outWidth, outHeight, Config.ARGB_8888)
            val canvas = Canvas(scaledBitmap)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawColor(Color.TRANSPARENT)
            val resizedBitmap = resizeImage(inputBitmap, outWidth, outHeight)
            if (resizedBitmap != null) {
                canvas.drawBitmap(resizedBitmap, 0f, 0f, paint)
            }
            scaledBitmap
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun resizeImage(bitmap: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        bitmap ?: return null
        return try {
            val scaledBitmap = createBitmap(newWidth, newHeight, Config.ARGB_8888)
            val ratioX = newWidth / bitmap.width.toFloat()
            val ratioY = newHeight / bitmap.height.toFloat()
            val middleX = newWidth / 2.0f
            val middleY = newHeight / 2.0f
            val scaleMatrix = Matrix()
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
            val canvas = Canvas(scaledBitmap)
            canvas.setMatrix(scaleMatrix)
            canvas.drawBitmap(
                bitmap,
                middleX - bitmap.width / 2,
                middleY - bitmap.height / 2,
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            scaledBitmap
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun sizeOf(data: Bitmap): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            data.rowBytes * data.height
        } else {
            data.byteCount
        }
    }
}