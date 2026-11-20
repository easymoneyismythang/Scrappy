package com.scrappy.app

import com.scrappy.app.data.DailyVerse
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DailyVerseTest {
    @Test fun stableOnSameDate() {
        val date=LocalDate.of(2026,9,30)
        assertEquals(DailyVerse.forDate(date),DailyVerse.forDate(date))
    }
    @Test fun changesAcrossMonthYearAndLeapDay() {
        listOf("2026-09-30","2026-12-31","2028-02-28","2028-02-29").forEach {
            val date=LocalDate.parse(it)
            assertNotEquals(DailyVerse.forDate(date),DailyVerse.forDate(date.plusDays(1)))
        }
    }
    @Test fun allQuotesAppearBeforeRepeating() {
        val start=LocalDate.of(2026,1,1)
        val count=DailyVerse.verses.size
        assertEquals(count,(0 until count).map{DailyVerse.forDate(start.plusDays(it.toLong()))}.toSet().size)
        assertEquals(DailyVerse.forDate(start),DailyVerse.forDate(start.plusDays(count.toLong())))
    }
}
