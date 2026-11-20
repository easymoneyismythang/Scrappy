package com.scrappy.app

import com.scrappy.app.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class SalesAnalyticsTest {
    private val zone=ZoneId.of("America/New_York")
    private fun sale(id: String="a", date: String="2026-10-03T12:00:00Z", yard: String="Yard A", payout: Double=100.0,
        expenses: Double=20.0, miles: Double=40.0, expected: Double=80.0)=
        SaleRecord(id,"estimate-$id",yard,Instant.parse(date).toEpochMilli(),payout,expenses,expected,10.0,miles,7.0)
    @Test fun dateFilterIsInclusive() {
        val f=SaleFilter(LocalDate.parse("2026-10-03"),LocalDate.parse("2026-10-03"))
        assertEquals(1,SalesAnalytics.filter(listOf(sale()),f,zone).size)
    }
    @Test fun endDateUsesLocalMidnight() {
        val f=SaleFilter(end=LocalDate.parse("2026-10-02"))
        assertEquals(1,SalesAnalytics.filter(listOf(sale(date="2026-10-03T03:59:59Z")),f,zone).size)
        assertTrue(SalesAnalytics.filter(listOf(sale(date="2026-10-03T04:00:00Z")),f,zone).isEmpty())
    }
    @Test fun invalidRangeRejected() {
        try { SalesAnalytics.filter(emptyList(),SaleFilter(LocalDate.of(2026,10,4),LocalDate.of(2026,10,3)),zone);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun exactYardIgnoresCaseAndWhitespace() {
        assertEquals(1,SalesAnalytics.filter(listOf(sale(yard=" Yard A "),sale("b",yard="Yard AB")),SaleFilter(yard="yard a"),zone).size)
    }
    @Test fun searchMatchesReceiptId() { assertEquals(1,SalesAnalytics.filter(listOf(sale("Unique")),SaleFilter(query="uniq"),zone).size) }
    @Test fun searchMatchesEstimateId() { assertEquals(1,SalesAnalytics.filter(listOf(sale()),SaleFilter(query="estimate-a"),zone).size) }
    @Test fun searchMatchesYard() { assertEquals(1,SalesAnalytics.filter(listOf(sale()),SaleFilter(query=" YARD "),zone).size) }
    @Test fun newestSort() {
        val rows=listOf(sale("a"),sale("b",date="2026-10-04T12:00:00Z"))
        assertEquals("b",SalesAnalytics.filter(rows,SaleFilter(),zone).first().id)
    }
    @Test fun oldestSort() {
        assertEquals("a",SalesAnalytics.filter(listOf(sale("b",date="2026-10-04T12:00:00Z"),sale()),SaleFilter(sort=SaleSort.OLDEST),zone).first().id)
    }
    @Test fun netSortAllowsLosses() {
        assertEquals("b",SalesAnalytics.filter(listOf(sale(expenses=200.0),sale("b")),SaleFilter(sort=SaleSort.HIGHEST_NET),zone).first().id)
    }
    @Test fun payoutSort() { assertEquals("b",SalesAnalytics.filter(listOf(sale(),sale("b",payout=200.0)),SaleFilter(sort=SaleSort.HIGHEST_PAYOUT),zone).first().id) }
    @Test fun weeklyGroupsStartMondayAcrossYear() {
        val rows=listOf(sale(date="2027-01-01T12:00:00Z"))
        assertEquals("2026-12-28",SalesAnalytics.periods(rows,SalePeriod.WEEK,zone).single().label)
    }
    @Test fun monthGroupsUseLocalDate() { assertEquals("2026-09",SalesAnalytics.periods(listOf(sale(date="2026-10-01T02:00:00Z")),SalePeriod.MONTH,zone).single().label) }
    @Test fun yearGroupsNewestFirst() {
        assertEquals(listOf("2027","2026"),SalesAnalytics.periods(listOf(sale(),sale("b",date="2027-03-01T12:00:00Z")),SalePeriod.YEAR,zone).map { it.label })
    }
    @Test fun yardGroupingNormalizesNames() {
        val group=SalesAnalytics.yards(listOf(sale(yard="Yard A"),sale("b",yard=" yard a "))).single()
        assertEquals(2,group.totals.count)
    }
    @Test fun totalsDoNotDeductModeledFuelTwice() {
        val t=SalesAnalytics.totals(listOf(sale()))
        assertEquals(80.0,t.net,0.0);assertEquals(70.0,t.afterLabor,0.0);assertEquals(7.0,t.fuel,0.0)
    }
    @Test fun averagesIncludeLosses() {
        val t=SalesAnalytics.totals(listOf(sale(),sale("b",payout=0.0,expenses=100.0)))
        assertEquals(50.0,t.averagePayout!!,0.0);assertEquals(-10.0,t.averageNet!!,0.0)
    }
    @Test fun perMileCalculations() {
        val t=SalesAnalytics.totals(listOf(sale()))
        assertEquals(2.0,t.netPerMile!!,0.0);assertEquals(0.5,t.expensePerMile!!,0.0)
    }
    @Test fun zeroDenominatorsAreUnavailable() {
        val t=SalesAnalytics.totals(listOf(sale(miles=0.0,expected=0.0)))
        assertNull(t.netPerMile);assertNull(t.expensePerMile);assertNull(t.payoutDifferencePercent)
        assertNull(SalesAnalytics.totals(emptyList()).averageNet)
    }
    @Test fun accuracyAndDstBoundary() {
        assertEquals(25.0,SalesAnalytics.totals(listOf(sale())).payoutDifferencePercent!!,0.0)
        val f=SaleFilter(LocalDate.of(2026,11,1),LocalDate.of(2026,11,1))
        val rows=listOf(sale("a",date="2026-11-01T05:30:00Z"),sale("b",date="2026-11-01T06:30:00Z"))
        assertEquals(2,SalesAnalytics.filter(rows,f,zone).size)
    }
}
