package com.kokoconnect.android.model.player

import java.util.*

class Language {
    var name: String = ""
    var code: String = ""
    var locale: Locale = Locale.ENGLISH

    constructor(language: String = "en") {
        this.code = language
        this.locale = Locale.Builder().setLanguage(name).build()
        this.name = locale.displayName
    }
}