package com.kokoconnect.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.kokoconnect.android.R
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.dimenAttr
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class TimelineItemView: AppCompatTextView,
    ShiftableOnScroll {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var progress: Int? by Delegates.observable(null) { _: KProperty<*>, _: Int?, newValue: Int? ->
        invalidate()
    }
    var isProgressEnabled: Boolean = false
    set(value) {
        field = value
        invalidate()
    }

    private val paint by lazy { Paint().apply {
        color = colorAttr(R.attr.colorOnSurfaceVariant7)
        strokeWidth = dimenAttr(R.attr.guideTimelinePositionMarkerWidth).toFloat()
    } }

    private val paddingTopForLine by lazy {
        0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isProgressEnabled) {
            progress?.let {
                val x = width / 100f * it.toFloat()
                canvas.drawLine(
                    x,
                    paddingTopForLine.toFloat(),
                    x,
                    height.toFloat() - paddingTopForLine,
                    paint
                );
            }
        }
    }

    override fun updateVisibleArea() {
        val padding = dimenAttr(R.attr.guideTextPadding)
        val startOffset = Math.max(padding, -left)
        setPadding(startOffset, 0, padding, 0)
    }
}