package com.kokoconnect.android.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kokoconnect.android.R
import java.lang.reflect.InvocationTargetException

fun Context.isSameOrientation(config: Configuration): Boolean {
    return resources.configuration.orientation == config.orientation
}

fun Configuration.isOrientationLandscape(): Boolean {
    return orientation == Configuration.ORIENTATION_LANDSCAPE
}
fun Configuration.isOrientationPortrait(): Boolean {
    return orientation == Configuration.ORIENTATION_PORTRAIT
}

fun Context.isOrientationLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun Context.isOrientationPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

object ActivityUtils {
    val ANIMATION_DURATION_SHORT =
        Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime).toLong()
    val ANIMATION_DURATION_MEDIUM =
        Resources.getSystem().getInteger(android.R.integer.config_mediumAnimTime).toLong()

    @ColorInt
    fun getColorFromAttr(
        context: Context,
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        context.theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    fun getIntFromAttr(
        context: Context,
        @AttrRes attrInt: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        context.theme.resolveAttribute(attrInt, typedValue, resolveRefs)
        return typedValue.data
    }

    fun getDimensionFromAttr(
        context: Context,
        @AttrRes attrDimen: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        val a = context.obtainStyledAttributes(typedValue.data, arrayOf(attrDimen).toIntArray())
        val dimen = a.getDimensionPixelSize(0, -1)
        a.recycle()
        return dimen
    }

    fun getDrawableFromAttr(
        context: Context,
        @AttrRes attrDrawable: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Drawable? {
        context.theme.resolveAttribute(attrDrawable, typedValue, resolveRefs)
        val imageResId = typedValue.resourceId
        return ContextCompat.getDrawable(context, imageResId)
    }

    fun getResIdFromAttr(
        context: Context,
        @AttrRes attrDrawable: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        context.theme.resolveAttribute(attrDrawable, typedValue, resolveRefs)
        return typedValue.resourceId
    }

    fun getOrientation(context: Context): Int = context.resources.configuration.orientation

    fun getVersionName(): String {
        return AppParams.versionName
    }

    fun getActionBarHeight(activity: Activity?): Int {
        var actionBarHeight = 0
        if (activity is AppCompatActivity) {
            actionBarHeight = activity?.supportActionBar?.height ?: 0
        }
        if (actionBarHeight != 0)
            return actionBarHeight
        val tv = TypedValue()
        if (activity == null) return 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (activity.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true))
                actionBarHeight =
                    TypedValue.complexToDimensionPixelSize(
                        tv.data,
                        activity.resources.displayMetrics
                    )
        } else if (activity.theme.resolveAttribute(R.attr.actionBarSize, tv, true))
            actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, activity.resources.displayMetrics)
        return actionBarHeight
    }

    fun getStatusBarHeight(activity: Activity?): Int {
        var result = 0
        val resourceId =
            activity?.resources?.getIdentifier("status_bar_height", "dimen", "android") ?: 0
        if (resourceId > 0) {
            result = activity?.resources?.getDimensionPixelSize(resourceId) ?: 0
        }
        return result
    }

    fun getNavigationBarSize(context: Context?): Int {
        if (context == null) return 0
        val appUsableSize = getAppUsableScreenSize(context)
        val realScreenSize = getRealScreenSize(context)

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return Point(realScreenSize.x - appUsableSize.x, appUsableSize.y).x
        }

        // navigation bar at the bottom
        return if (appUsableSize.y < realScreenSize.y) {
            Point(appUsableSize.x, realScreenSize.y - appUsableSize.y).x
        } else Point().x

        // navigation bar is not present
    }

    private fun getAppUsableScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    private fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size)
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = Display::class.java.getMethod("getRawWidth").invoke(display) as Int
                size.y = Display::class.java.getMethod("getRawHeight").invoke(display) as Int
            } catch (e: IllegalAccessException) {
            } catch (e: InvocationTargetException) {
            } catch (e: NoSuchMethodException) {
            }

        }
        return size
    }

    fun hideKeyboard(activity: Activity?) {
        val view = activity?.findViewById<View>(android.R.id.content)
        if (view != null) {
            val imm =
                activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}