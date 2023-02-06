package com.kokoconnect.android.model.event

import com.google.gson.internal.bind.util.ISO8601Utils
import java.util.*

abstract class AmsEvent(val type: String)
abstract class EventData {
    var created_at: String = (ISO8601Utils.format(Date())).replace("Z", "")
    var timer_duration: Long = 0L
}