package com.kokoconnect.android.util

import androidx.exifinterface.media.ExifInterface
import kotlin.math.absoluteValue

class Orientation() {
    var flipVertical = false
    var flipHorizontal = false
    var rotation: Float = 0f

    constructor(rotation: Float, flipHorizontal: Boolean, flipVertical: Boolean) : this() {
        this.rotation = rotation
        this.flipHorizontal = flipHorizontal
        this.flipVertical = flipVertical
    }

    fun hasRotation(): Boolean {
        return rotation.absoluteValue > 0.01f
    }

    constructor(orientationExif: Int) : this() {
        when (orientationExif) {
            ExifInterface.ORIENTATION_NORMAL -> {

            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotation = 90f
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotation = 180f
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotation = -90f
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                flipVertical = true
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                rotation = 90f
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotation = -90f
                flipHorizontal = true
            }
            else -> {

            }
        }
    }
}