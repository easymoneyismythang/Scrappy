package com.scrappy.app

import com.scrappy.app.data.*
import org.junit.Assert.*
import org.junit.Test

class LedgerToolsTest {
    private val metal=ScrappyRepository.metals.first().name
    private val d=LoadDraft(listOf(LoadItem("i",metal,10.0,2.0,"Yard",1L)),TripCosts(40.0,20.0,3.5))
    private val e=LoadEstimate("e","Load",1L,d)
    private val sale=SaleRecord("s","e","Yard",2L,30.0,7.0,20.0,5.0,40.0,7.0)
    private val snapshot=LedgerSnapshot(d,listOf(e),listOf(sale))
    @Test fun renameRetainsIdentityAndSnapshot() {
        val next=LedgerTools.rename(e," Renamed ");assertEquals("Renamed",next.name);assertEquals(e.id,next.id);assertEquals(d,next.draft)
    }
    @Test fun invalidRenameRejected() { try { LedgerTools.rename(e," ");fail() } catch(_:IllegalArgumentException) {} }
    @Test fun duplicateGetsNewIdsAndCapturedPrices() {
        val next=LedgerTools.duplicate(e,"Copy",3L)
        assertNotEquals(e.id,next.id);assertNotEquals(d.items.single().id,next.draft.items.single().id)
        assertEquals(d.items.single().pricePerLb,next.draft.items.single().pricePerLb,0.0);assertEquals(3L,next.savedAt)
    }
    @Test fun correctionPreservesHistoricalGrossAndLabor() {
        val next=LedgerTools.correct(sale," Other ",40.0,10.0,80.0,3L,4L)
        assertEquals("s",next.id);assertEquals("Other",next.yard);assertEquals(14.0,next.modeledFuel,0.0)
        assertEquals(20.0,next.estimatedGross,0.0);assertEquals(5.0,next.estimatedLabor,0.0);assertEquals(30.0,next.cashNet,0.0)
    }
    @Test fun correctionRejectsFutureDate() {
        try { LedgerTools.correct(sale,"Yard",1.0,0.0,0.0,5L,4L);fail() } catch(_:IllegalArgumentException) {}
        try { TripLedger.receipt(e,"Yard",1.0,0.0,5L,now=4L);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun zeroMilesCorrectionDoesNotInventFuelRate() {
        val next=LedgerTools.correct(sale.copy(roundTripMiles=0.0,modeledFuel=0.0),"Yard",30.0,7.0,10.0,2L,3L)
        assertEquals(0.0,next.modeledFuel,0.0)
    }
    @Test fun scaleAndCapacityBounds() {
        assertEquals(25.0,LedgerTools.scale(d,2.5).items.single().pounds,0.0)
        assertEquals(d.costs,LedgerTools.scale(d,2.5).costs)
        try { LedgerTools.scale(d,0.0);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun copyRowAndRejectFullDraft() {
        assertEquals(2,LedgerTools.copyItem(d,"i").items.size)
        val full=d.copy(items=(1..100).map { d.items.single().copy(id="$it") })
        try { LedgerTools.copyItem(full,"1");fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun repriceRequiresEveryGradeAndRetainsWeight() {
        val next=LedgerTools.reprice(d,listOf(PriceQuote(metal,3.0,"New Yard",10L)))
        assertEquals(3.0,next.items.single().pricePerLb,0.0);assertEquals(10.0,next.items.single().pounds,0.0)
        assertEquals("New Yard",next.items.single().source)
        try { LedgerTools.reprice(d,emptyList());fail() } catch(_:IllegalStateException) {}
    }
    @Test fun consolidateOnlyIdenticalQuoteProvenance() {
        val input=d.copy(items=d.items+listOf(d.items.single().copy(id="b"),d.items.single().copy(id="c",source="Other")))
        val next=LedgerTools.consolidate(input)
        assertEquals(2,next.items.size);assertEquals(TripLedger.totals(input).gross,TripLedger.totals(next).gross,0.0)
    }
    @Test fun sortAndClearCostsKeepItems() {
        assertEquals(d.items,LedgerTools.clearCosts(d).items);assertEquals(TripCosts(),LedgerTools.clearCosts(d).costs)
        val names=ScrappyRepository.metals.map { it.name }.sortedDescending().take(2)
        val unsorted=d.copy(items=names.mapIndexed { index,name -> d.items.single().copy(id="row-$index",metal=name) })
        val sorted=LedgerTools.sorted(unsorted)
        assertEquals(names.sorted(),sorted.items.map { it.metal })
        assertEquals(TripLedger.totals(unsorted).gross,TripLedger.totals(sorted).gross,0.0)
    }
    @Test fun mergeAddsAndIsIdempotent() {
        val incoming=snapshot.copy(estimates=listOf(e.copy(id="e2")),sales=listOf(sale.copy(id="s2",estimateId="e2")))
        val preview=LedgerTools.merge(snapshot,incoming)
        assertEquals(1,preview.addedEstimates);assertEquals(1,preview.addedSales)
        val again=LedgerTools.merge(preview.snapshot,incoming)
        assertEquals(0,again.addedEstimates);assertEquals(1,again.skippedSales)
    }
    @Test fun mergeRejectsConflictingEstimateAndSaleIds() {
        for(incoming in listOf(snapshot.copy(estimates=listOf(e.copy(name="Changed"))),snapshot.copy(sales=listOf(sale.copy(actualPayout=40.0)))))
            try { LedgerTools.merge(snapshot,incoming);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun mergeRejectsOverCapacity() {
        val full=snapshot.copy(sales=(1..200).map { sale.copy(id="$it") })
        try { LedgerTools.merge(full,snapshot);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun mergeDraftChoiceIsExplicit() {
        val incoming=snapshot.copy(draft=LoadDraft())
        assertEquals(d,LedgerTools.merge(snapshot,incoming).snapshot.draft)
        assertEquals(LoadDraft(),LedgerTools.merge(snapshot,incoming,true).snapshot.draft)
    }
    @Test fun utf8BomAndMalformedBytes() {
        assertEquals("hello",LedgerTools.strictUtf8("\uFEFFhello".toByteArray(Charsets.UTF_8)))
        try { LedgerTools.strictUtf8(byteArrayOf(0xC3.toByte(),0x28));fail() } catch(_:java.nio.charset.CharacterCodingException) {}
    }
    @Test fun spreadsheetCsvHasBomAndNeutralizesFormula() {
        val csv=LedgerTools.csvForSpreadsheet(listOf(sale.copy(yard="=HYPERLINK(\"bad\")")))
        assertTrue(csv.startsWith("\uFEFFid,"));assertTrue(csv.contains("\"'=HYPERLINK"))
    }
    @Test fun mergeRoundTripRetainsOrphanReceipt() {
        val orphan=snapshot.copy(estimates=emptyList())
        val merged=LedgerTools.merge(LedgerSnapshot(LoadDraft(),emptyList(),emptyList()),orphan).snapshot
        assertEquals(merged,LedgerBackup.decode(LedgerBackup.encode(merged)))
    }
}
