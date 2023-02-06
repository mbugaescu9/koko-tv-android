package com.kokoconnect.android.ui.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout


/**
 * A FrameLayout that allow setting a delegate for intercept touch event
 */
class InterceptTouchFrameLayout : FrameLayout {
    private var mDisallowIntercept = false

    interface OnInterceptTouchEventListener {
        /**
         * If disallowIntercept is true the touch event can't be stealed and the return value is ignored.
         * @see android.view.ViewGroup.onInterceptTouchEvent
         */
        fun onInterceptTouchEvent(
            view: InterceptTouchFrameLayout?,
            ev: MotionEvent?,
            disallowIntercept: Boolean
        ): Boolean

        /**
         * @see android.view.View.onTouchEvent
         */
        fun onTouchEvent(
            view: InterceptTouchFrameLayout?,
            event: MotionEvent?
        ): Boolean
    }

    private class DummyInterceptTouchEventListener :
        OnInterceptTouchEventListener {
        override fun onInterceptTouchEvent(
            view: InterceptTouchFrameLayout?,
            ev: MotionEvent?,
            disallowIntercept: Boolean
        ): Boolean {
            return false
        }

        override fun onTouchEvent(
            view: InterceptTouchFrameLayout?,
            event: MotionEvent?
        ): Boolean {
            return false
        }
    }

    private var mInterceptTouchEventListener =
        DUMMY_LISTENER

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context!!, attrs, defStyleAttr) {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyle: Int
    ) : super(context!!, attrs, defStyleAttr, defStyle) {
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        parent.requestDisallowInterceptTouchEvent(disallowIntercept)
        mDisallowIntercept = disallowIntercept
    }

    fun setOnInterceptTouchEventListener(interceptTouchEventListener: OnInterceptTouchEventListener?) {
        mInterceptTouchEventListener =
            interceptTouchEventListener ?: DUMMY_LISTENER
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val stealTouchEvent =
            mInterceptTouchEventListener.onInterceptTouchEvent(this, ev, mDisallowIntercept)
        return stealTouchEvent && !mDisallowIntercept || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = mInterceptTouchEventListener.onTouchEvent(this, event)
        return handled || super.onTouchEvent(event)
    }

    companion object {
        private val DUMMY_LISTENER: OnInterceptTouchEventListener =
            DummyInterceptTouchEventListener()
    }
}