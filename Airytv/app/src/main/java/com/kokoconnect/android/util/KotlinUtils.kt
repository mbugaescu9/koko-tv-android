package com.kokoconnect.android.util

import android.net.Uri
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


object KotlinUtils {

}


fun Uri.hasQueryParameter(parameter: String): Boolean {
    val parameterValue = this.getQueryParameter(parameter)
    return parameterValue != null
}

fun Uri.isQueryParameterEmpty(parameter: String): Boolean {
    val parameterValue = this.getQueryParameter(parameter)
    return parameterValue != null && parameterValue.isEmpty()
}

fun Uri.replaceQueryParameter(key: String, newValue: String): Uri {
    val params = queryParameterNames
    val newUri = buildUpon().clearQuery()
    for (param in params) {
        newUri.appendQueryParameter(
            param,
            if (param == key) {
                newValue
            } else {
                getQueryParameter(param)
            }
        )
    }
    return newUri.build()
}

fun Uri.addQueryParameter(key: String, newValue: String): Uri {
    val params = queryParameterNames
    val newUri = buildUpon().clearQuery()
    var isParamExists = false
    for (param in params) {
        val value = if (param == key) {
            isParamExists = true
            newValue
        } else {
            getQueryParameter(param)
        }
        newUri.appendQueryParameter(param, value)
    }
    if (!isParamExists) {
        newUri.appendQueryParameter(key, newValue)
    }
    return newUri.build()
}

fun AppCompatActivity.findNavControllerFixed(fragmentContainerViewId: Int): NavController? {
    return (supportFragmentManager.findFragmentById(fragmentContainerViewId) as? NavHostFragment)?.navController
}

@ColorInt
fun Context.getAttrColor(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun Context.getAttrInt(
    @AttrRes attrInt: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrInt, typedValue, resolveRefs)
    return typedValue.data
}

fun Context.getAttrDimension(
    @AttrRes attrDimen: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    val a = obtainStyledAttributes(typedValue.data, arrayOf(attrDimen).toIntArray())
    val dimen = a.getDimensionPixelSize(0, -1)
    a.recycle()
    return dimen
}

fun Context.getAttrDrawable(
    @AttrRes attrDrawable: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Drawable? {
    theme.resolveAttribute(attrDrawable, typedValue, resolveRefs)
    val imageResId = typedValue.resourceId
    return ContextCompat.getDrawable(this, imageResId)
}

fun Context.getAttrResId(
    @AttrRes attrDrawable: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrDrawable, typedValue, resolveRefs)
    return typedValue.resourceId
}

fun <T> Continuation<T>.resumeSafe(result: T) {
    try {
        this.resume(result)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun <T> Continuation<T>.resumeWithSafe(result: Result<T>) {
    try {
        this.resumeWith(result)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun NavController.navigateSafe(actionId: Int) {
    try {
        this.navigate(actionId)
    } catch (ex: java.lang.IllegalArgumentException) {
        ex.printStackTrace()
    }
}


class FragmentViewBindingDelegate<T : ViewBinding?>(
    val fragment: Fragment,
    val bindingInflater: (LayoutInflater) -> T
) : ReadOnlyProperty<Fragment, T?> {
    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            val viewLifecycleOwnerLiveDataObserver =
                Observer<LifecycleOwner?> {
                    val viewLifecycleOwner = it ?: return@Observer
                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            binding = null
                        }
                    })
                }

            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observeForever(
                    viewLifecycleOwnerLiveDataObserver
                )
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.removeObserver(
                    viewLifecycleOwnerLiveDataObserver
                )
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
        val binding = binding
        if (binding != null) {
            return binding
        }

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            return null
        }

        return bindingInflater.invoke(LayoutInflater.from(fragment.context))
            .also { this.binding = it }
        //viewBindingFactory.(thisRef.requireView()).also { this.binding = it }
    }
}

fun <T : ViewBinding> Fragment.viewBinding(bindingInflater: (LayoutInflater) -> T) =
    FragmentViewBindingDelegate(this, bindingInflater)

fun <T : ViewBinding> View.viewBinding(bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        try {
            bindingInflater.invoke(LayoutInflater.from(context))
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

inline fun <T : ViewBinding?> AppCompatActivity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        try {
            bindingInflater.invoke(layoutInflater)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

fun <T> List<T>.containsAny(otherList: List<T>): Boolean {
    var containsAny = false
    for (other in otherList) {
        containsAny = containsAny || this.contains(other)
        if (containsAny) break
    }
    return containsAny
}