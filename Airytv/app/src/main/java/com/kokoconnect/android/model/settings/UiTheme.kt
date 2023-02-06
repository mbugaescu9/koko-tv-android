package com.kokoconnect.android.model.settings

import com.kokoconnect.android.R

enum class UiTheme(val themeNumber: Int, val prefName: String, val themeName: String, val resId: Int) {
    UI_THEME_NEW(1, "UI_THEME_NEW", "New", R.style.Theme_New),
    UI_THEME_CLASSIC(0,"UI_THEME_CLASSIC", "Classic", R.style.Theme_Classic)
}