package com.kokoconnect.android.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.ActivityUserAlertBinding
import com.kokoconnect.android.model.notification.UserAlert
import com.bumptech.glide.Glide
import com.google.gson.Gson
import org.jetbrains.anko.browse
import org.jetbrains.anko.contentView

const val USER_ALERT_DATA = "USER_ALERT_DATA"

class UserAlertActivity : AppCompatActivity() {

    private var userAlert: UserAlert? = null
    private var binding: ActivityUserAlertBinding? = null

    private fun prepareUserAlert(): UserAlert? {
        val json = intent.getStringExtra(USER_ALERT_DATA)
        return try {
            Gson().fromJson<UserAlert>(
                json,
                UserAlert::class.java
            )
        } catch (ex: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_alert)
        contentView?.let {
            binding = ActivityUserAlertBinding.bind(it)
        }
        userAlert = prepareUserAlert()
        binding?.imageViewBackground?.let {
            Glide.with(this)
                .load(userAlert?.imageUrl)
                .into(it)
        }
        if (userAlert?.textOnScreen?.isNotEmpty() == true) {
            binding?.textViewAlertText?.text = userAlert?.textOnScreen
        }
        binding?.buttonOpenLink?.apply {
            text = userAlert?.textOnButton
            setOnClickListener {
                browse(userAlert?.linkUrl ?: return@setOnClickListener)
            }
        }
        binding?.imageViewButtonClose?.apply {
            visibility = if (userAlert?.showX == true) View.VISIBLE else View.GONE
            setOnClickListener {
                onBackPressed()
            }
        }
    }

    override fun onBackPressed() {
        if (userAlert?.showX == true) {
            super.onBackPressed()
        }
    }
}
