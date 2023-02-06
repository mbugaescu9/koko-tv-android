package com.kokoconnect.android.ui.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.kokoconnect.android.R

class GiveawaysButton : FrameLayout {
    companion object {
        const val ANIMATION_TRANSITION_DURATION = 300L
        const val ANIMATION_DELAY_DURATION = 1000L
    }

    var mainView: View? = null
    var tvFree: TextView? = null
    var tvGifts: TextView? = null

    private var animationEnabled = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    fun init(context: Context, attrs: AttributeSet?) {
        val view = LayoutInflater.from(context).inflate(R.layout.view_giveaway_button, parent as? ViewGroup, false)
        mainView = view
        tvFree = mainView?.findViewById<TextView>(R.id.tvFree)
        tvGifts = mainView?.findViewById<TextView>(R.id.tvGifts)
        var textColor: Int? = null
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.GiveawaysButton, 0, 0)
            try {
                textColor = ta.getColor(R.styleable.GiveawaysButton_textColor, 0)
            } finally {
                ta.recycle()
            }
        }
        textColor?.let{
            tvFree?.setTextColor(textColor)
            tvGifts?.setTextColor(textColor)
        }

        addView(view)
        isFocusable = true
        isClickable = true

        startAnimation()
    }

    fun startAnimation() {
        if (animationEnabled) {
            return
        } else {
            animationEnabled = true
        }


//        val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
//        fadeInAnimation.startOffset = ANIMATION_DELAY_DURATION
//        fadeInAnimation.duration = ANIMATION_TRANSITION_DURATION
//        val fadeOutAnimation = AlphaAnimation(1.0f, 0.0f)
//        fadeOutAnimation.startOffset = ANIMATION_DELAY_DURATION
//        fadeOutAnimation.duration = ANIMATION_TRANSITION_DURATION

        val tvFreeFadeOut = ObjectAnimator.ofFloat(tvFree, View.ALPHA, 1.0f, 0.0f)
        tvFreeFadeOut?.duration =
            ANIMATION_TRANSITION_DURATION
        tvFreeFadeOut?.startDelay =
            ANIMATION_DELAY_DURATION

        val tvGiftsFadeIn = ObjectAnimator.ofFloat(tvGifts, View.ALPHA, 0.0f, 1.0f)
        tvGiftsFadeIn?.duration =
            ANIMATION_TRANSITION_DURATION
        tvGiftsFadeIn?.startDelay =
            ANIMATION_DELAY_DURATION

        val tvFreeFadeIn = ObjectAnimator.ofFloat(tvFree, View.ALPHA, 0.0f, 1.0f)
        tvFreeFadeIn?.duration =
            ANIMATION_TRANSITION_DURATION
        tvFreeFadeIn?.startDelay =
            ANIMATION_DELAY_DURATION

        val tvGiftsFadeOut = ObjectAnimator.ofFloat(tvGifts, View.ALPHA, 1.0f, 0.0f)
        tvGiftsFadeOut?.duration =
            ANIMATION_TRANSITION_DURATION
        tvGiftsFadeOut?.startDelay =
            ANIMATION_DELAY_DURATION

        val animation1 = AnimatorSet()
        val animation2 = AnimatorSet()
        animation1.playTogether(tvFreeFadeOut, tvGiftsFadeIn)
        animation2.playTogether(tvFreeFadeIn, tvGiftsFadeOut)

        animation1.addListener(object: Animator.AnimatorListener {
            override fun onAnimationRepeat(animator: Animator) {
                if (!animationEnabled) {
                    animator?.cancel()
                }
            }
            override fun onAnimationEnd(p0: Animator) {
                if (animationEnabled) {
                    animation2.start()
                }
            }
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationStart(p0: Animator) {}
        })
        animation2.addListener(object: Animator.AnimatorListener {
            override fun onAnimationRepeat(animator: Animator) {
                if (!animationEnabled) {
                    animator?.cancel()
                }
            }
            override fun onAnimationEnd(p0: Animator) {
                if (animationEnabled) {
                    animation1.start()
                }
            }
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationStart(p0: Animator) {}
        })
        animation1.start()
    }


    fun stopAnimation() {
        animationEnabled = false
    }
}