package com.kokoconnect.android.util

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.absoluteValue
import kotlin.math.max

class GlideScaleResizeTransformation(var destWidth: Int, var destHeight: Int) : BitmapTransformation() {
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        var result: Bitmap? = null
        toTransform.let{ bitmap ->
            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()

            val scaleFactor = max(destWidth.toFloat() / width, destHeight.toFloat() / height)
            val scaledWidth = (width*scaleFactor).toInt()
            val scaledHeight = (height*scaleFactor).toInt()
            val scaleResult = ImageUtils.resizeBitmap(bitmap, scaledWidth, scaledHeight)

//            Timber.d("Scaled width = ${scaledWidth} height = ${scaledHeight} scaleFactor = ${scaleFactor}")
            //Glide recycle bitmap
//            if (scaleResult != bitmap) {
//                bitmap.recycle()
//            }

            val paddingTop = ((scaledHeight - destHeight).absoluteValue / 2).toInt()
            val paddingLeft = ((scaledWidth - destWidth).absoluteValue / 2).toInt()

//            Timber.d("New width = ${destWidth} height = ${destHeight} paddingLeft ${paddingTop} paddingTop ${paddingLeft}")

            val cutResult = ImageUtils.cutBitmap(scaleResult, paddingLeft, paddingTop, destWidth, destHeight)
            if (cutResult != scaleResult) {
                scaleResult.recycle()
            }
            result = cutResult
        }
        return result ?: Bitmap.createBitmap(0,0, Bitmap.Config.ARGB_4444)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest?.update("scale resize transformation".toByteArray())
    }
}
