package jp.uhimania.qrreader.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

sealed class DateFormat {
    object Today : DateFormat()
    data class DaysAgo(val day: Int) : DateFormat()
    data class MonthsAgo(val month: Int) : DateFormat()
    data class Date(val date: String) : DateFormat()
}

class FormatDateUseCase {
    operator fun invoke(date: Date): DateFormat {
        val now = Date()
        val diff = abs(now.time - date.time)
        if (date.year() == now.year() && date.month() == now.month() && date.day() == now.day()) {
            return DateFormat.Today
        } else if (diff <= 30L * 24 * 60 * 60 * 1000) {
            return DateFormat.DaysAgo(now.dayOfYear() - date.dayOfYear())
        } else if (diff <= 365L * 24 * 60 * 60 * 1000) {
            val months = if (date.month() > now.month()) now.month() + 12 else now.month()
            return DateFormat.MonthsAgo(months - date.month())
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return DateFormat.Date(formatter.format(date))
        }
    }
}

fun Date.year(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.YEAR)
}

fun Date.month(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.MONTH) + 1
}

fun Date.day(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.DAY_OF_MONTH)
}

fun Date.dayOfYear(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.DAY_OF_YEAR)
}
