package com.scrappy.app

import com.scrappy.app.data.*
import org.junit.Assert.*
import org.junit.Test

class TripLedgerTest {
    private val now=1_800_000_000_000L
    private fun sample()=LoadDraft(listOf(
        TripLedger.item("Copper #1",10.0,"lb",listOf(PriceQuote("Copper #1",4.0,"Real yard",now))),
        TripLedger.item("Steel",100.0,"lb",emptyList())),TripCosts(40.0,20.0,3.0,60.0,20.0,2.0))
    private fun estimate()=LoadEstimate("estimate-1","Truck load",now,sample())
    @Test fun mixedLoadSubtractsFuelLaborAndFees() {
        val t=TripLedger.totals(sample())
        assertEquals(110.0,t.pounds,1e-9);assertEquals(49.0,t.gross,1e-9)
        assertEquals(6.0,t.fuel,1e-9);assertEquals(20.0,t.labor,1e-9)
        assertEquals(21.0,t.net,1e-9);assertEquals(28.0,t.breakEvenPayout,1e-9)
    }
    @Test fun kilogramConversionPreservesValue() {
        val i=TripLedger.item("Copper #1",1.0,"kg",listOf(PriceQuote("Copper #1",4.0,"Yard",now)))
        assertEquals(2.2046226218487757,i.pounds,1e-12);assertEquals(8.818490487395103,i.value,1e-10)
    }
    @Test fun quoteIsCapturedAndNotRepriced() {
        val i=TripLedger.item("Copper #1",10.0,"lb",listOf(PriceQuote("Copper #1",4.0,"Yard",now)))
        val newer=PriceQuote("Copper #1",5.0,"Other yard",now+1)
        assertEquals(5.0,TripLedger.item("Copper #1",10.0,"lb",listOf(newer)).pricePerLb,1e-9)
        assertEquals(40.0,TripLedger.totals(LoadDraft(listOf(i))).gross,1e-9)
        assertEquals("Yard",i.source);assertEquals(now,i.quoteTime)
    }
    @Test fun demoItemIsExplicitAndZeroQuoteIsPreserved() {
        assertEquals("Demo benchmark",TripLedger.item("Steel",1.0,"lb",emptyList()).source)
        val i=TripLedger.item("Steel",1.0,"lb",listOf(PriceQuote("Steel",0.0,"Yard",now)))
        assertEquals(0.0,i.value,0.0);assertEquals("Yard",i.source)
    }
    @Test fun editWeightRetainsSourceAndPrice() {
        val i=sample().items.first();val changed=i.copy(pounds=20.0)
        assertEquals(80.0,changed.value,1e-9);assertEquals(i.source,changed.source);assertEquals(i.quoteTime,changed.quoteTime)
    }
    @Test fun negativeNetIsNotClampedToZero() {
        assertEquals(-28.0,TripLedger.totals(sample().copy(items=emptyList())).net,1e-9)
    }
    @Test fun zeroCostTripWorks() {
        val t=TripLedger.totals(sample().copy(costs=TripCosts()))
        assertEquals(t.gross,t.net,1e-9)
    }
    @Test fun invalidUnitsWeightsAndGradesAreRejected() {
        listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,1_000_001.0).forEach {
            assertTrue(runCatching { TripLedger.pounds(it,"lb") }.isFailure)
        }
        assertTrue(runCatching { TripLedger.pounds(1.0,"oz") }.isFailure)
        assertTrue(runCatching { TripLedger.item("Unknown",1.0,"lb",emptyList()) }.isFailure)
    }
    @Test fun invalidCostsAreRejected() {
        listOf(TripCosts(mpg=0.0),TripCosts(mpg=Double.NaN),TripCosts(fees=-1.0),
            TripCosts(fuelPerGallon=Double.POSITIVE_INFINITY),TripCosts(roundTripMiles=-1.0)).forEach {
            assertTrue(runCatching { TripLedger.totals(LoadDraft(costs=it)) }.isFailure)
        }
    }
    @Test fun duplicateItemIdsAndOversizedLoadsAreRejected() {
        val i=sample().items.first()
        assertTrue(runCatching { TripLedger.validate(LoadDraft(listOf(i,i))) }.isFailure)
        assertTrue(runCatching { TripLedger.validate(LoadDraft((0..100).map { i.copy(id=it.toString()) })) }.isFailure)
    }
    @Test fun saleSeparatesCashNetFromEstimatedLabor() {
        val s=TripLedger.receipt(estimate()," Real yard ",55.0,8.0,now,now=now)
        assertEquals("Real yard",s.yard);assertEquals(47.0,s.cashNet,1e-9)
        assertEquals(27.0,s.afterLabor,1e-9);assertEquals(6.0,s.payoutDifference,1e-9)
    }
    @Test fun receiptKeepsActualMilesAndModelsFuelWithoutDeductingItTwice() {
        val s=TripLedger.receipt(estimate(),"Yard",55.0,12.0,now,60.0,now=now)
        assertEquals(60.0,s.roundTripMiles,1e-9);assertEquals(9.0,s.modeledFuel,1e-9)
        assertEquals(43.0,s.cashNet,1e-9)
        assertTrue(runCatching { TripLedger.receipt(estimate(),"Yard",55.0,8.0,now,-1.0,now=now) }.isFailure)
    }
    @Test fun emptyEstimateAndInvalidSaleAreRejected() {
        assertTrue(runCatching { TripLedger.validateEstimate(estimate().copy(draft=LoadDraft())) }.isFailure)
        assertTrue(runCatching { TripLedger.receipt(estimate()," ",55.0,8.0,now,now=now) }.isFailure)
        assertTrue(runCatching { TripLedger.receipt(estimate(),"Yard",-1.0,8.0,now,now=now) }.isFailure)
        assertTrue(runCatching { TripLedger.receipt(estimate(),"Yard",55.0,Double.NaN,now,now=now) }.isFailure)
    }
    @Test fun zeroPayoutAndLossAreAllowed() {
        val s=TripLedger.receipt(estimate(),"Yard",0.0,8.0,now,now=now)
        assertEquals(-8.0,s.cashNet,1e-9);assertEquals(-28.0,s.afterLabor,1e-9)
    }
    @Test fun csvEscapesCommaQuotesAndNewlines() {
        val s=TripLedger.receipt(estimate(),"A, \"B\"\nYard",55.0,8.0,now,now=now)
        val csv=TripLedger.salesCsv(listOf(s))
        assertTrue(csv.contains("\"A, \"\"B\"\"\nYard\""));assertTrue(csv.contains(",55.0,8.0,47.0,20.0,27.0,49.0,6.0"))
    }
    @Test fun csvNeutralizesFormulaPrefixes() {
        listOf("=SUM(1,1)","+cmd","-cmd","@cmd","  =cmd","\tcmd","\rcmd").forEach { yard ->
            val csv=TripLedger.salesCsv(listOf(TripLedger.receipt(estimate(),yard,1.0,0.0,now,now=now)))
            // Receipt trims input, so tab/CR input becomes a harmless plain name.
            if(yard.trim().first() in listOf('=','+','-','@')) assertTrue(csv.contains("\"'"+yard.trim()))
        }
    }
}
