package com.kokoconnect.android.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import java.io.UnsupportedEncodingException
import java.lang.reflect.InvocationTargetException
import java.util.*

object DeviceUtils {
    private const val PREFS_DEVICE_ID_FILE = "device_id"
    private const val PREFS_DEVICE_ID_KEY = "device_id"
    private const val INVALID_ANDROID_ID = "9774d56d682e549c"
    private var deviceUUID: UUID? = null

    fun getIDFA(context: Context): String {
        var idInfo: AdvertisingIdClient.Info? = null
        try {
            idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
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
        return advertId
    }

    fun getRealDisplaySize(context: Context): Point {
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

    fun getDisplaySize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return getDisplaySize(windowManager)
    }

    fun getDisplaySize(windowManager: WindowManager): Point {
        val display: Display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        return metrics
    }

    fun getDisplayDpi(context: Context): Float {
        val metrics = context.resources.displayMetrics
        val densityDpi = (metrics.density * 160f)
        return densityDpi
    }

    fun getDeviceRamMb(context: Context): Float {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMemoryMB = memInfo.totalMem / 1048576.0f
        return totalMemoryMB
    }

    @SuppressLint("HardwareIds")
    fun getDeviceUUID(context: Context): UUID {
        if (deviceUUID == null) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_DEVICE_ID_FILE, 0)
            val savedDeviceId = prefs.getString(PREFS_DEVICE_ID_KEY, null)
            if (savedDeviceId != null) {
                deviceUUID = UUID.fromString(savedDeviceId)
            } else {
                val androidId: String = try {
                    Settings.Secure.getString(
                        context.contentResolver, Settings.Secure.ANDROID_ID
                    )
                } catch (ex: Exception) {
                    INVALID_ANDROID_ID
                }
                // Use the Android ID unless it's broken, in which case
                // fallback on deviceId,
                // unless it's not available, then fallback on a random
                // number which we store to a prefs file
                deviceUUID = try {
                    if (INVALID_ANDROID_ID != androidId) {
                        UUID.nameUUIDFromBytes(
                            androidId.toByteArray(charset("utf8"))
                        )
                    } else {
                        UUID.randomUUID()
                    }
                } catch (e: UnsupportedEncodingException) {
                    UUID.randomUUID()
                }
                // Write the value out to the prefs file
                prefs.edit().putString(PREFS_DEVICE_ID_KEY, deviceUUID.toString()).apply()
            }
        }
        return deviceUUID!!
    }

    fun getCameraExists(context: Context): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
}