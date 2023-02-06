package com.kokoconnect.android.repo

import android.content.Context
import android.content.SharedPreferences
import com.kokoconnect.android.model.settings.UiTheme
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*
import kotlin.properties.Delegates

class Preferences(val context : Context) {

    private var pref : SharedPreferences by Delegates.notNull()

    init {
        pref = context.defaultSharedPreferences
    }

    inner class UI {

        fun setTheme(theme: UiTheme) {
            val editor = pref.edit()
            editor.putString("ui_theme", theme.prefName)
            editor.apply()
        }

        fun getTheme(): UiTheme {
            val name = pref.getString("ui_theme", null) ?: UiTheme.UI_THEME_NEW.prefName
            return UiTheme.valueOf(name)
        }

    }

    inner class Guide {
        fun getChannelId() = pref.getInt("guide_channel_Id", -1)

        fun setChannelId(id: Int) {
            val editor = pref.edit()
            editor.putInt("guide_channel_Id", id)
            editor.apply()
        }

        fun getChromecastChannelNumber() = pref.getInt("guide_cast_channel_number", -1)

        fun setChromecastChannelNumber(number: Int?) {
            val editor = pref.edit()
            editor.putInt("guide_cast_channel_number", number ?: -1)
            editor.apply()
        }

        fun clearChromecastData() {
            setChromecastChannelNumber(null)
        }
    }

    inner class Ams {
        fun getAmsId() = pref.getString("ams_id", null)

        fun setAmsId(ams_id: String?) {
            val editor = pref.edit()
            editor.putString("ams_id", ams_id)
            editor.apply()
        }
    }

    inner class Giveaways {
        fun isNeedInfoDialog(): Boolean = pref.getBoolean("giveaways_info_dialog", true)

        fun setNeedInfoDialog(value: Boolean) {
            val editor = pref.edit()
            editor.putBoolean("giveaways_info_dialog", value)
            editor.apply()
        }
    }

    inner class Auth {
        fun getToken(): String? = pref.getString("auth_token", null)

        fun setToken(token: String?) {
            val editor = pref.edit()
            editor.putString("auth_token", token)
            editor.apply()
        }

        fun getEmail(): String? = pref.getString("auth_email", null)

        fun setEmail(email: String?) {
            val editor = pref.edit()
            editor.putString("auth_email", email)
            editor.apply()
        }
    }

    inner class ArchiveOrg {
        fun getCookies(): String? = pref.getString("archive_org_cookies", null)

        fun setCookies(newCookies: String?) {
            val editor = pref.edit()
            editor.putString("archive_org_cookies", newCookies)
            editor.apply()
        }

        fun getAuthKey() : String? = pref.getString("archive_org_auth_key", null)

        fun setAuthKey(newApiKey: String?) {
            val editor = pref.edit()
            editor.putString("archive_org_auth_key", newApiKey)
            editor.apply()
        }
    }

    inner class Rating {
        fun getRatingDialogSendRating() = pref.getBoolean("rating_dialog_send_rating", false)

        fun setRatingDialogSendRating(value: Boolean) {
            val editor = pref.edit()
            editor.putBoolean("rating_dialog_send_rating", value)
            editor.apply()
        }

        fun getDateLastCheck(): Date? {
            val unixtime = pref.getLong("rating_dialog_date_last_check", 0L)
            if (unixtime == 0L) return null
            return Date(unixtime)
        }

        fun setDateLastCheck(date: Date) {
            val editor = pref.edit()
            editor.putLong("rating_dialog_date_last_check", date.time)
            editor.apply()
        }

        fun isFirstDialog(): Boolean = pref.getBoolean("is_first_dialog", true)

        fun setFirstDialog(isFirstDialog: Boolean) {
            val editor = pref.edit()
            editor.putBoolean("is_first_dialog", isFirstDialog)
            editor.apply()
        }

        fun getSecondsOfUse() = pref.getInt("seconds_of_use", 0)

        fun setSecondsOfUse(seconds: Int) {
            val editor = pref.edit()
            editor.putInt("seconds_of_use", seconds)
            editor.apply()
        }

        fun clearSecondsOfUse() {
            val editor = pref.edit()
            editor.putInt("seconds_of_use", 0)
            editor.apply()
        }
    }
}