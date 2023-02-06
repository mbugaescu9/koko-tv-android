package com.kokoconnect.android.util

import android.graphics.*

object ImageUtils {

    fun roundBitmapCorners(image: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, image.width, image.height)
        val rectF = RectF(rect)

        val color = 0xffffffff
        paint.isAntiAlias = true
        paint.color = color.toInt()
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(image, rect, rect, paint)
        return output
    }

    fun cutBitmap(image: Bitmap, x: Int, y: Int, width: Int, height: Int) : Bitmap {
        val result = Bitmap.createBitmap(image, x, y, width, height)
        return result
    }

    fun resizeBitmap(image: Bitmap, width: Int, height: Int) : Bitmap{
        val result = Bitmap.createScaledBitmap(image, width, height, false)
        return result
    }
}