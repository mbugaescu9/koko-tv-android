package com.kokoconnect.android.ui.fragment

import androidx.fragment.app.Fragment
import com.kokoconnect.android.ui.activity.BaseActivity

abstract class BaseFragment: Fragment() {

    fun openThemeDialog() {
        (activity as? BaseActivity)?.openThemeDialog()
    }

}