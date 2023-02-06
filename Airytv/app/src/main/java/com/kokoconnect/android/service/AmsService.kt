package com.kokoconnect.android.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.kokoconnect.android.api.*
import com.kokoconnect.android.model.event.*
import com.kokoconnect.android.repo.Preferences
import com.kokoconnect.android.util.DeviceUtils
import com.google.gson.Gson
import dagger.android.AndroidInjection
import retrofit2.Call
import retrofit2.Callback
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt


class AmsService : Service() {
    companion object {
        const val EVENTS_TIMER_INTERVAL_SEC = 10L
        const val EVENTS_TIMER_TICK_SEC = 1L
    }
    private var eventsStack: Queue<AmsEvent> = LinkedList<AmsEvent>()
    @Inject
    lateinit var eventsServer: AmsEventsService
    private var userInfo: AmsUserInfoResponse? = null
    private var deviceInfo = AmsDeviceInfo()

    var timerDelaySec = 0L
    private set
    private val mBinder = AmsBinder()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val runnable = Runnable {
        try {
            timerDelaySec += EVENTS_TIMER_TICK_SEC
            if (timerDelaySec >= EVENTS_TIMER_INTERVAL_SEC) {
                Timber.d("AmsService Runnable: start")
                timerDelaySec = 0
                if (eventsStack.size > 0) {
                    val arrayForSend = ArrayList<AmsEvent>(eventsStack)
                    val eventsListString = arrayForSend.map { it.type }.joinToString(" ")
                    Timber.d("AmsService Runnable: sending ${arrayForSend.size} events (${eventsListString})")
                    eventsStack.removeAll(arrayForSend)
                    sendEvents(arrayForSend)
                } else {
                    Timber.d("AmsService Runnable: eventsStack empty")
                }
            }
        } catch (ex: NullPointerException) {
            Timber.e("AmsService Runnable: ${ex.message}}")
        }
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        getUserInfo()
        prepareDeviceInfo()
        executor.scheduleWithFixedDelay(runnable, EVENTS_TIMER_TICK_SEC, EVENTS_TIMER_TICK_SEC, TimeUnit.SECONDS)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.d("Lifecycle onBind")
        return mBinder
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        sendBrowserEventNow(
            BrowseEventData(
                null,
                "airy://exit",
                null,
                null,
                null
            )
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            runnable.run()
            executor.shutdown()
            executor.awaitTermination(15, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            // ignore
        }
    }

    inner class AmsBinder : Binder() {
        internal
        val service: AmsService
            get() = this@AmsService
    }

    private fun getUserInfo() {
        val ams = eventsServer ?: return
        Thread {
            try {
                val lastId = Preferences(this@AmsService).Ams().getAmsId()
                val email = Preferences(this@AmsService).Auth().getEmail()
                ams.getUserInfo(email, lastId).enqueue(object : Callback<AmsUserInfo> {
                    override fun onResponse(
                        call: Call<AmsUserInfo>,
                        response: retrofit2.Response<AmsUserInfo>
                    ) {
                        userInfo = response.body()?.response
                        Preferences(this@AmsService).Ams().setAmsId(userInfo?.ams_id)
                    }

                    override fun onFailure(call: Call<AmsUserInfo>, t: Throwable) {
                        println()
                    }
                })
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun updateUser() {
        getUserInfo()
    }

    private fun sendEvents(events: List<AmsEvent>) {
        val ams = eventsServer ?: return
        Thread {
            try {
                val amsBody = getAmsBody(events)
                Timber.v("Events raw: ${Gson().toJson(amsBody)}")
                ams.sendEvents(amsBody).enqueue(object : Callback<AmsResponse> {
                    override fun onResponse(
                        call: Call<AmsResponse>,
                        response: retrofit2.Response<AmsResponse>
                    ) {
                        println()
                        Timber.v("AmsService Runnable: events send successfully, response code ${response.code()}")
                    }

                    override fun onFailure(call: Call<AmsResponse>, t: Throwable) {
                        println()
                        Timber.e("AmsService Runnable: events send failed!")
                        t.printStackTrace()
                    }
                })
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun getAmsBody(eventsList: List<AmsEvent>): AmsBody {
        val email = Preferences(this@AmsService).Auth().getEmail()
        val body = AmsBody(
            city = userInfo?.city,
            ams_id = userInfo?.ams_id,
            user_type = userInfo?.user_type,
            first_access = userInfo?.first_access,
            ip = userInfo?.ip,
            region = userInfo?.region,
            os = deviceInfo.os,
            language = deviceInfo.language,
            country = userInfo?.country,
            timezone = deviceInfo.timezone,
            events = eventsList,
            email = email
        )
        return body
    }

    private fun prepareDeviceInfo() {
        deviceInfo.cpu_make = Build.HARDWARE
        deviceInfo.cpu_model = Build.BOARD
        deviceInfo.dpi = getDeviceDpi()
        deviceInfo.language = Locale.getDefault().toString()
        deviceInfo.manufacturer = Build.MANUFACTURER
        deviceInfo.mobile_model = Build.MODEL
        deviceInfo.os = getAndroidVersion()
        deviceInfo.ram = getDeviceRam()
        deviceInfo.timezone = getTimezone()
        deviceInfo.screen_resolution = getScreenResolution()
    }

    private fun getTimezone(): Int {
        return TimeUnit.HOURS.convert(
            TimeZone.getDefault().rawOffset.toLong(), TimeUnit.MILLISECONDS
        ).toInt()
    }

    private fun getDeviceDpi(): String? {
        val densityDpi = DeviceUtils.getDisplayDpi(applicationContext).roundToInt()
        return "$densityDpi"
    }

    private fun getDeviceRam(): String? {
        val totalMemoryMB = DeviceUtils.getDeviceRamMb(applicationContext).roundToInt()
        return "${totalMemoryMB}"
    }

    private fun getAndroidVersion(): String {
        val release = Build.VERSION.RELEASE
        val sdkVersion = Build.VERSION.SDK_INT
        return "android: $release (SDK $sdkVersion)"
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }

    private fun capitalize(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }

    private fun getScreenResolution(): String {
        val point = DeviceUtils.getRealDisplaySize(applicationContext)
        val width = Math.min(point.x, point.y)
        val height = Math.max(point.x, point.y)
        return "${width}x$height"
    }

    fun calcTimerDuration() = EVENTS_TIMER_INTERVAL_SEC - timerDelaySec

    fun getExternalIp(): String? = userInfo?.ip

    fun sendWatchEvent(data: WatchEventData) {
        val watchEvent = WatchEvent(
            data.apply {
                timer_duration = calcTimerDuration()
            }
        )
        eventsStack.offer(watchEvent)
    }

    fun sendBrowserEvent(data: BrowseEventData) {
        eventsStack.offer(BrowseEvent(data.apply {
            timer_duration = calcTimerDuration()
        }))
    }

    fun sendStaticTimerEvent(data: StaticTimerEventData) {
        eventsStack.offer(StaticTimerEvent(data.apply {
            timer_duration = calcTimerDuration()
        }))
    }

    fun sendAdvertisementEvent(data: AdvertisementEventData) {
        eventsStack.offer(AdvertisementEvent(data.apply {
            timer_duration = calcTimerDuration()
        }))
    }

    fun sendLandingEvent(data: LandingEventData) {
        eventsStack.offer(LandingEvent(data.apply {
            timer_duration = calcTimerDuration()
        }))
    }

    fun sendRatingEvent(data: RatingEventData) {
        eventsStack.offer(RatingEvent(data.apply {
            timer_duration = calcTimerDuration()
        }))
    }

    fun sendGiveawaysEvent(data: GiveawaysEventData) {
        eventsStack.offer(GiveawaysEvent(data.apply {
            timer_duration = calcTimerDuration()
        }))
    }

    fun sendBrowserEventNow(data: BrowseEventData) {
        sendEvents(listOf<AmsEvent>(BrowseEvent(data.apply {
            timer_duration = 0 //cause we send event immediately
        })))
    }
}