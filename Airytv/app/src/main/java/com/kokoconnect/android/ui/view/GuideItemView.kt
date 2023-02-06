package com.kokoconnect.android.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.kokoconnect.android.R
import org.jetbrains.anko.dimenAttr

class GuideItemView: AppCompatTextView,
    ShiftableOnScroll {
    private val padding: Int by lazy { dimenAttr(R.attr.guideTextPadding) }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun updateVisibleArea() {
        val startOffset = Math.max(0, -left)
        setPadding(startOffset + padding, 0, padding, 0)
    }
}