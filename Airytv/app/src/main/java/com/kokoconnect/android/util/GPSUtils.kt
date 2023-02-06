package com.kokoconnect.android.util

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.kokoconnect.android.AiryTvApp
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import timber.log.Timber


object GPSUtils {
    private const val GPS_AVAILABILITY_REQUEST_CODE = 32935 // just random number

    val googlePlayServicesStarted = MutableLiveData<Boolean>(false)
    val googleAdsId = MutableLiveData<String>()

    init {
        googlePlayServicesStarted.observeForever(object : Observer<Boolean> {
            override fun onChanged(isStarted: Boolean?) {
                Timber.d("googlePlayServicesStarted onChanged() ${isStarted}")
                if (isStarted == true) {
                    Thread {
                        googleAdsId.postValue(getIDFA())
                    }.start()
                    googlePlayServicesStarted.removeObserver(this)
                }
            }
        })
        googleAdsId.observeForever {
            Timber.d("googleAdsId == $it")
        }
    }

    fun getGoogleAdsId(): String? {
        return googleAdsId.value
    }


    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        checkGooglePlayServices(activity, requestCode)
    }

    fun checkGooglePlayServices(activity: Activity) {
        Timber.d("checkGooglePlayServices()")
        checkGooglePlayServices(activity, GPS_AVAILABILITY_REQUEST_CODE)
    }

    private fun checkGooglePlayServices(activity: Activity, onActivityResultCode: Int) {
        if (onActivityResultCode == GPS_AVAILABILITY_REQUEST_CODE) {
            checkGooglePlayServicesAvailability(activity, GPS_AVAILABILITY_REQUEST_CODE, Runnable {
                googlePlayServicesStarted.postValue(true)
            })
        }
    }

    private fun checkGooglePlayServicesAvailability(
        activity: Activity,
        googlePlayServicesAvailabilityRequestCode: Int,
        onSuccess: Runnable
    ) {
        val googlePlayServicesAvailabilityResult =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        if (googlePlayServicesAvailabilityResult == ConnectionResult.SUCCESS) {
            onSuccess.run()
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(
                activity,
                googlePlayServicesAvailabilityResult,
                googlePlayServicesAvailabilityRequestCode,
                null
            )?.show()
        }
    }

    private fun getIDFA(): String {
        var idInfo: AdvertisingIdClient.Info? = null
        try {
            idInfo = AdvertisingIdClient.getAdvertisingIdInfo(AiryTvApp.instance)
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var advertId = ""
        try {
            advertId = idInfo?.getId() ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Timber.d("getIDFA() ${advertId}")
        return advertId
    }
}