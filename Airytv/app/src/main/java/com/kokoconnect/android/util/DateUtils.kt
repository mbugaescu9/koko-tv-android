package com.kokoconnect.android.util

import fr.turri.jiso8601.Iso8601Deserializer
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    fun getDateFormatter(pattern: String, locale: Locale = Locale.ENGLISH): SimpleDateFormat {
        return SimpleDateFormat(pattern, locale)
    }


    fun formatDate(date: Long, pattern: String, locale: Locale = Locale.ENGLISH): String? {
        return try {
            getDateFormatter(pattern, locale).format(Date(date))
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun formatDate(date: Date, pattern: String, locale: Locale = Locale.ENGLISH): String? {
        return try {
            getDateFormatter(pattern, locale).format(date)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun parseIsoDate(timeString: String?): Long {
        var parsedDate = 0L
        try {
            parsedDate = Iso8601Deserializer.toDate(timeString).time
        } catch (ex: RuntimeException) {
            ex.printStackTrace()
        }
        return parsedDate
    }

    fun getCurrentDate(): Long {
        return Date().time
    }

    fun getCurrentTimeInSecs(): Int {
        return (getCurrentDate() / 1000).toInt()
    }

    fun getCurrentTime(): Long {
        val cal = Calendar.getInstance()
//        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dateWithoutTime = cal.time.time
        return Date().time - dateWithoutTime
    }

    fun formatDuration(secs: Int): String {
        val duration = Duration.standardSeconds(secs.toLong())
        val periodFormatter = if (duration.toStandardHours().hours > 0) {
            PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(1)
                .appendHours()
                .appendSuffix("h")
                .appendSeparator(" ")
                .appendMinutes()
                .appendSuffix("m")
        } else {
            PeriodFormatterBuilder()
                .appendMinutes()
                .appendSuffix("m")
        }
        return periodFormatter.toFormatter().print(duration.toPeriod())
    }

    fun getTimezoneString(): String {
        val timezoneOffsetMillis =
            TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
        val timezoneOffsetHours = TimeUnit.MILLISECONDS.toHours(timezoneOffsetMillis)
        return timezoneOffsetHours.toStringWithSign()
    }
}

private fun Int.toStringWithSign(): String = if (this >= 0) "+$this" else toString()
private fun Long.toStringWithSign(): String = if (this >= 0) "+$this" else toString()