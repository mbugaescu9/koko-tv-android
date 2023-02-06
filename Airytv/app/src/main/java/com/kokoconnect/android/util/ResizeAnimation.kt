package com.kokoconnect.android.util


import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

class ResizeAnimation(
    var view: View,
    val targetHeight: Int,
    var startHeight: Int
) : Animation() {

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        val newHeight = (startHeight + (targetHeight - startHeight) * interpolatedTime).toInt()
        view.layoutParams.height = newHeight
        view.requestLayout()
    }

    override fun initialize(width: Int, height: Int, parentWidth: Int, parentHeight: Int) {
        super.initialize(width, height, parentWidth, parentHeight)
    }

    override fun willChangeBounds(): Boolean {
        return true
    }
}
