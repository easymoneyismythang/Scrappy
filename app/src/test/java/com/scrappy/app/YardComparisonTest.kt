package com.scrappy.app

import com.scrappy.app.data.*
import org.junit.Assert.*
import org.junit.Test

class YardComparisonTest {
    private val metal=ScrappyRepository.metals.first().name
    private val other=ScrappyRepository.metals[1].name
    private val now=200_000_000L
    private val load=LoadDraft(listOf(LoadItem("row",metal,100.0,9.0,"Original",1L)))
    private fun quote(yard: String="A", price: Double=2.0, time: Long=now)=PriceQuote(metal,price,yard,time)
    private fun trip(yard: String="A", miles: Double=40.0)=YardTrip(yard,TripCosts(miles,20.0,4.0,60.0,20.0,3.0))
    private fun compare(q: List<PriceQuote> = listOf(quote()), t: List<YardTrip> = listOf(trip()))=YardComparison.compare(load,q,t,now)
    private fun rejects(block: () -> Unit) { try { block();fail("Expected rejection") } catch(_:IllegalArgumentException) {} }
    @Test fun wholeLoadUsesYardQuotesAndPreservesOriginal() {
        val d=load.copy(items=load.items+load.items.first().copy(id="second",pounds=50.0,metal=other))
        val result=YardComparison.compare(d,listOf(quote(),PriceQuote(other,1.0,"A",now)),listOf(trip()),now).single()
        assertEquals(250.0,result.totals!!.gross,0.0);assertEquals(219.0,result.totals!!.net,0.0)
        assertEquals(9.0,d.items.first().pricePerLb,0.0);assertEquals("row",result.draft!!.items.first().id)
    }
    @Test fun latestQuoteIsScopedToNormalizedYard() {
        val result=compare(listOf(quote(" a ",1.0,now-1),quote("A",2.0),quote("B",100.0,now+1)))
        assertEquals(200.0,result.single().totals!!.gross,0.0)
    }
    @Test fun invalidQuotesAreIgnored() { assertNull(compare(listOf(quote(price=Double.NaN))).single().totals) }
    @Test fun missingGradeDoesNotFallBackToDemoOrOtherYard() {
        val result=compare(listOf(quote("B"))).single()
        assertNull(result.draft);assertEquals(listOf(metal),result.missingGrades)
    }
    @Test fun ranksByNetRatherThanGross() {
        val result=compare(listOf(quote("A",2.0),quote("B",2.1)),listOf(trip("A"),trip("B",200.0)))
        assertEquals(listOf("A","B"),result.map { it.yard })
    }
    @Test fun incompleteYardSortsAfterNegativeNet() {
        val result=compare(listOf(quote(price=0.0)),listOf(trip("Missing"),trip()))
        assertEquals("A",result.first().yard);assertEquals(-31.0,result.first().totals!!.net,0.0)
    }
    @Test fun tiesHaveStableAlphabeticOrder() {
        assertEquals(listOf("A","B"),compare(listOf(quote("A"),quote("B")),listOf(trip("B"),trip("A"))).map { it.yard })
    }
    @Test fun flagsStaleAtExactly24Hours() {
        assertEquals(listOf(metal),compare(listOf(quote(time=now-86_400_000L))).single().staleGrades)
        assertTrue(compare(listOf(quote(time=now-86_399_999L))).single().staleGrades.isEmpty())
    }
    @Test fun futureQuoteIsExplicitAndNotStale() {
        val result=compare(listOf(quote(time=now+1))).single()
        assertEquals(listOf(metal),result.futureGrades);assertTrue(result.staleGrades.isEmpty())
    }
    @Test fun costsAndBreakEvenIncludeFuelLaborAndFees() {
        val t=compare().single().totals!!
        assertEquals(8.0,t.fuel,0.0);assertEquals(20.0,t.labor,0.0);assertEquals(3.0,t.fees,0.0)
        assertEquals(31.0,t.breakEvenPayout,0.0);assertEquals(169.0,t.net,0.0)
    }
    @Test fun reportContainsSpreadAndProvenance() {
        val report=YardComparison.report(compare(listOf(quote("A"),quote("B",1.0)),listOf(trip("A"),trip("B"))))
        assertTrue(report.contains("$100.00"));assertTrue(report.contains("$2.0000/lb (quote:"))
        assertTrue(report.contains("No ledger changes made"))
    }
    @Test fun incompleteReportNeverInventsPayout() {
        val report=YardComparison.report(compare(emptyList()))
        assertTrue(report.contains("Not ranked"));assertFalse(report.contains("Gross:"))
    }
    @Test fun yardListNormalizesDuplicatesAndRejectsBadQuotes() {
        assertEquals(listOf("A","B"),YardComparison.yards(listOf(quote(" A "),quote("a"),quote("B"),quote("Bad",-1.0))))
    }
    @Test fun invalidAndDuplicateTripsRejected() {
        rejects { compare(t=listOf(trip("A"),trip(" a "))) }
        rejects { compare(t=listOf(trip(miles=Double.NaN))) }
        rejects { compare(t=listOf(YardTrip("A",TripCosts(mpg=0.0)))) }
        rejects { compare(t=listOf(trip(" "))) }
    }
    @Test fun emptyLoadAndTooManyYardsRejected() {
        rejects { YardComparison.compare(LoadDraft(),listOf(quote()),listOf(trip()),now) }
        rejects { compare(t=emptyList()) }
        rejects { compare(t=(1..21).map { trip(it.toString()) }) }
    }
}
