package com.kokoconnect.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kokoconnect.android.R
import com.kokoconnect.android.adapter.tv.pixelsPerSecond
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.dimenAttr
import java.util.*

class GlobalTimelineView : View {
    var scrollOffsetX = 0
        set(value) {
            field = value
            invalidate()
        }
    var fromTime: Long = 0
        set(value) {
            field = value
            invalidate()
        }
    var toTime: Long = 0
        set(value) {
            field = value
            invalidate()
        }
    var currentTime: Long = 0

    private val paint by lazy {
        Paint().apply {
            color = colorAttr(R.attr.colorOnSurfaceVariant7)
            strokeWidth = dimenAttr(R.attr.guideTimelinePositionMarkerWidth).toFloat()
        }
    }

    private val paddingLeftForLine by lazy {
        dimenAttr(R.attr.guideChannelItemWidth).toFloat()
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setWillNotDraw(false)
        timeTick()
    }

    fun timeTick() {
        currentTime = calculateCurrentTime()
        invalidate()
    }

    private fun calculateCurrentTime() = Calendar.getInstance().timeInMillis

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        var positionX = paddingLeftForLine - scrollOffsetX
        positionX += ((currentTime - fromTime).toFloat() / 1000f) * context.pixelsPerSecond()
        val positionY = 0f
        if (positionX in paddingLeftForLine..width.toFloat()) {
            val height = height.toFloat()
            canvas?.drawLine(positionX, positionY, positionX, height, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // not handling touch events
        return false
    }
}