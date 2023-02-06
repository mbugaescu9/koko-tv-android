package com.kokoconnect.android.util

import android.content.Context
import android.view.animation.Interpolator
import android.widget.Scroller

class FixedSpeedScroller: Scroller {
    var fixedDuration: Int = 1000
    private set

    constructor(duration: Int, context: Context): super(context) {
        this.fixedDuration = duration
    }

    constructor(duration: Int, context: Context, interpolator: Interpolator) : super(context, interpolator) {
        this.fixedDuration = duration
    }

    constructor(duration: Int, context: Context, interpolator: Interpolator, flywheel: Boolean): super(context, interpolator, flywheel) {
        this.fixedDuration = duration
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        // Ignore received duration, use fixed one instead
        super.startScroll(startX, startY, dx, dy, this.fixedDuration)
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        // Ignore received duration, use fixed one instead
        super.startScroll(startX, startY, dx, dy, this.fixedDuration)
    }
}