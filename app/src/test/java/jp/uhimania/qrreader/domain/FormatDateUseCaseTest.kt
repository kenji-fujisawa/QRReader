package jp.uhimania.qrreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormatDateUseCaseTest {
    @Test
    fun testInvoke() {
        val useCase = FormatDateUseCase()

        var calendar = Calendar.getInstance()
        assertEquals(DateFormat.Today, useCase(calendar.time))

        val hours = calendar.get(Calendar.HOUR_OF_DAY) + 1
        calendar.add(Calendar.HOUR_OF_DAY, hours * -1)
        assertEquals(DateFormat.DaysAgo(1), useCase(calendar.time))

        calendar = Calendar.getInstance()

        val days = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.add(Calendar.DAY_OF_MONTH, days * -1)
        assertEquals(DateFormat.DaysAgo(days), useCase(calendar.time))

        calendar = Calendar.getInstance()

        calendar.add(Calendar.MONTH, -1)
        assertEquals(DateFormat.MonthsAgo(1), useCase(calendar.time))

        calendar = Calendar.getInstance()

        val months = calendar.get(Calendar.MONTH) + 1
        calendar.add(Calendar.MONTH, months * -1)
        assertEquals(DateFormat.MonthsAgo(months), useCase(calendar.time))

        calendar = Calendar.getInstance()

        calendar.add(Calendar.YEAR, -1)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        assertEquals(DateFormat.Date(formatter.format(calendar.time)), useCase(calendar.time))
    }
}