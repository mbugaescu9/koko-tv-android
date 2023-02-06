package com.kokoconnect.android.ui.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.FrameLayout
import com.kokoconnect.android.R

class RoundedCornerLayout : FrameLayout {

    private var maskBitmap: Bitmap? = null
    private var paint: Paint? = null
    private var maskPaint: Paint? = null
    private var cornerRadius: Float = 0f
    private var displayMetrics: DisplayMetrics? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        maskPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        displayMetrics = context.resources.displayMetrics

        val a: TypedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedCornerLayout, 0, 0)
        val value = a.getDimension(R.styleable.RoundedCornerLayout_radius, 0f)

        setCornerRadius(value)
        setWillNotDraw(false)
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius, displayMetrics)
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val offscreenBitmap =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val offscreenCanvas = Canvas(offscreenBitmap)

        super.draw(offscreenCanvas)

        maskBitmap = maskBitmap ?: createMask(width, height)
        offscreenCanvas.drawBitmap(maskBitmap!!, 0f, 0f, maskPaint)
        canvas.drawBitmap(offscreenBitmap, 0f, 0f, paint)
    }

    private fun createMask(width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(mask)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            cornerRadius,
            cornerRadius,
            paint
        )

        return mask
    }

}