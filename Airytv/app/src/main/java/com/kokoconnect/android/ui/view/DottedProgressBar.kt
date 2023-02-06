package com.kokoconnect.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.kokoconnect.android.R
import kotlin.math.roundToInt

class DottedProgressBar : View {
    private var mDotSize: Float = 0f
    private var mSpacing: Float = 0f
    private var mJumpingSpeed: Long = 0
    private var mEmptyDotsColor: Int = 0
    private var mActiveDotColor: Int = 0
    private var mActiveDot: Drawable? = null
    private var mInactiveDot: Drawable? = null

    private var isInProgress = false
    private var isActiveDrawable = false
    private var isInactiveDrawable = false

    private var mActiveDotIndex = 0

    private var mMaxDotsNumber = -1
    private var mDotsNumber = 3
    private var mCounter = 0
    private var mPaint: Paint? = null
    private var mPaddingLeft: Int = 0
    private var mHandler: Handler = Handler()
    private var onFinished: (() -> Unit)? = null
    private var mRunnable = object : Runnable {
        override fun run() {
            onFinished?.let{
                if (isFullCycle()) {
                    stopProgress(it)
                }
            }
            if (mDotsNumber != 0) {
                mActiveDotIndex = (mActiveDotIndex + 1) % mDotsNumber
            }
            mCounter++
            this@DottedProgressBar.invalidate()
            mHandler.postDelayed(this, mJumpingSpeed)
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    fun init(attrs: AttributeSet){
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DottedProgressBar,
            0, 0
        )

        isInProgress = false

        try {
            val value = TypedValue()

            a.getValue(R.styleable.DottedProgressBar_activeDot, value)
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                isActiveDrawable = false
                mActiveDotColor = ContextCompat.getColor(context, value.resourceId)
            } else if (value.type == TypedValue.TYPE_STRING) {
                isActiveDrawable = true
                mActiveDot = ContextCompat.getDrawable(context, value.resourceId)
            }

            a.getValue(R.styleable.DottedProgressBar_inactiveDot, value)
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                isInactiveDrawable = false
                mEmptyDotsColor = ContextCompat.getColor(context, value.resourceId)
            } else if (value.type == TypedValue.TYPE_STRING) {
                isInactiveDrawable = true
                mInactiveDot = ContextCompat.getDrawable(context, value.resourceId)
            }

            mDotSize = a.getDimensionPixelSize(R.styleable.DottedProgressBar_dotSize, 5).toFloat()
            mSpacing = a.getDimensionPixelSize(R.styleable.DottedProgressBar_spacing, 10).toFloat()

            mActiveDotIndex = a.getInt(R.styleable.DottedProgressBar_activeDotIndex, 0)

            mJumpingSpeed = a.getInt(R.styleable.DottedProgressBar_jumpingSpeed, 500).toLong()
            mMaxDotsNumber = a.getInt(R.styleable.DottedProgressBar_maxDots, -1)

            mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
        } finally {
            a.recycle()
            scaleSizeValues()
            invalidate()
        }
    }

    private fun scaleSizeValues(){
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        for (i in 0 until mDotsNumber) {
            val x =
                (paddingLeft + mPaddingLeft + i * (mSpacing + mDotSize)).roundToInt()
            if (isInactiveDrawable) {
                mInactiveDot?.apply {
                    setBounds(
                        x,
                        paddingTop,
                        (x + mDotSize).roundToInt(),
                        (paddingTop + mDotSize).roundToInt()
                    )
                    draw(canvas)
                }
            } else {
                mPaint?.let { paint ->
                    paint.color = mEmptyDotsColor
                    canvas.drawCircle(
                        x + mDotSize / 2f,
                        paddingTop + mDotSize / 2f,
                        mDotSize / 2f, paint
                    )
                }
            }
        }
        if (isInProgress) {
            val x =
                (paddingLeft + mPaddingLeft + mActiveDotIndex * (mSpacing + mDotSize)).roundToInt()
            if (isActiveDrawable) {
                mActiveDot?.apply {
                    setBounds(
                        x,
                        paddingTop,
                        (x + mDotSize).roundToInt(),
                        (paddingTop + mDotSize).roundToInt()
                    )
                    draw(canvas)
                }
            } else {
                mPaint?.let { paint ->
                    paint.color = mActiveDotColor
                    canvas.drawCircle(
                        x + mDotSize / 2f,
                        paddingTop + mDotSize / 2f, mDotSize / 2f, paint
                    )
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize (widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize (heightMeasureSpec)

        val widthWithoutPadding = parentWidth - paddingLeft - paddingRight
        val heigthWithoutPadding = parentHeight - paddingTop - paddingBottom

        val calculatedHeight = paddingTop + paddingBottom + mDotSize.roundToInt()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(parentWidth, calculatedHeight)
        mDotsNumber = calculateDotsNumber(widthWithoutPadding)
    }

    private fun calculateDotsNumber(width: Int): Int {
        var number = (width / (mDotSize + mSpacing)).roundToInt()
        if (mMaxDotsNumber >= 0 && number > mMaxDotsNumber){
            number = mMaxDotsNumber
        }
        mPaddingLeft = (width - (mDotSize * number + mSpacing * (number-1)).roundToInt()) / 2
        //setPadding(getPaddingLeft() + (int) mPaddingLeft, getPaddingTop(), getPaddingRight() + (int) mPaddingLeft, getPaddingBottom());
        return number
    }

    private fun isFullCycle(): Boolean = mCounter >= mMaxDotsNumber

    fun startProgress() {
        if (!isInProgress) {
            isInProgress = true
            mActiveDotIndex = -1
            mHandler.removeCallbacksAndMessages(null)
            mHandler.post(mRunnable)
        }
    }

    fun stopProgress(onFinished: (() -> Unit)) {
        if (isInProgress) {
            if (isFullCycle()) {
                isInProgress = false
                mHandler.removeCallbacksAndMessages(null)
                invalidate()
                onFinished.invoke()
            } else {
                this.onFinished = onFinished
            }
        }
    }

}