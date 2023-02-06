package com.kokoconnect.android.ui.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.ActivityInterstitialPlaceholderBinding
import com.kokoconnect.android.model.ads.interstitial.InitInterstitialPlaceholder
import com.kokoconnect.android.model.ads.interstitial.placeholder.InterstitialAd
import org.jetbrains.anko.contentView

class InterstitialPlaceholderActivity : AppCompatActivity(){

    lateinit var interstitialAd: InterstitialAd
    var countDownTimeLeft: Long = 0
    private set
    var closeEnabled = false
    private set
    private var countDownTimer: CountDownTimer? = null
    private var binding: ActivityInterstitialPlaceholderBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        setContentView(R.layout.activity_interstitial_placeholder)
        contentView?.let {
            binding = ActivityInterstitialPlaceholderBinding.bind(it)
        }

        val interstitialAdInstance = InitInterstitialPlaceholder.instanceInterstitialAd
        if (interstitialAdInstance != null) {
            interstitialAd = interstitialAdInstance
        } else {
            finish()
        }
        countDownTimeLeft = interstitialAd.currentPlaceholder?.duration ?: 0L
        interstitialAd.adListener?.onAdOpened()
        init()
    }

    override fun onBackPressed() {
        closeAd()
    }

    override fun onPause() {
        super.onPause()
        stopCountdown()
    }

    override fun onResume() {
        super.onResume()
        startCountdown()
    }

    private fun init(){
        showAd()
    }

    private fun onAdClosed() {
        interstitialAd.adListener?.onAdClosed()
    }

    private fun showAd(){
        binding?.closeButton?.setOnClickListener {
            closeAd()
        }
        interstitialAd.currentPlaceholder?.let{ data ->
            data.text?.let{
                binding?.adTextView?.setText(it)
            }
            data.title?.let{
                binding?.adTitleView?.setText(it)
            }
            data.imageBitmap?.let{
                binding?.adImageView?.setImageBitmap(it)
            }
            binding?.adImageView?.setOnClickListener {
                openUrl()
            }
        }
    }

    private fun openUrl(){
        interstitialAd.adListener?.onAdClicked()
        interstitialAd.currentPlaceholder?.url?.let{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(it)
            startActivity(intent)
        }
    }

    private fun startCountdown(){
        if (countDownTimeLeft > 0) {
            binding?.timerTextView?.visibility = View.VISIBLE
            binding?.closeButton?.visibility = View.INVISIBLE
            binding?.timerTextView?.setText(countDownTimeLeft.toString())

            countDownTimer = object : CountDownTimer(countDownTimeLeft.secToMs(), 1000L) {
                override fun onFinish() {
                    onCountdownFinish()
                }

                override fun onTick(millisUntilFinished: Long) {
                    onCountdownTick(millisUntilFinished)
                }
            }.start()
        } else {
            onCountdownFinish()
        }
    }

    private fun stopCountdown(){
        countDownTimer?.cancel()
    }

    private fun closeAd(){
        onAdClosed()
        if (closeEnabled) {
            finish()
        }
    }

    private fun onCountdownFinish(){
        closeEnabled = true
        binding?.closeButton?.visibility = View.VISIBLE
        binding?.timerTextView?.visibility = View.INVISIBLE
    }

    private fun onCountdownTick(millisUntilFinished: Long){
        countDownTimeLeft = millisUntilFinished.msToSec()
        binding?.timerTextView?.setText(countDownTimeLeft.toString())
    }
}

fun Long.msToSec(): Long{
    return this/1000L
}

fun Long.secToMs(): Long{
    return this*1000L
}