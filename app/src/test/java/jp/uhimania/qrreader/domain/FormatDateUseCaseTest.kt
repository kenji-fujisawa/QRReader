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

        val calendar = Calendar.getInstance()
        assertEquals(DateFormat.Today, useCase(calendar.time))

        calendar.add(Calendar.DAY_OF_MONTH, -1)
        assertEquals(DateFormat.DaysAgo(1), useCase(calendar.time))

        calendar.add(Calendar.MONTH, -1)
        assertEquals(DateFormat.MonthsAgo(1), useCase(calendar.time))

        calendar.add(Calendar.YEAR, -1)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        assertEquals(DateFormat.Date(formatter.format(calendar.time)), useCase(calendar.time))
    }
}