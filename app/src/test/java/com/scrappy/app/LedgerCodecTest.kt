package com.scrappy.app

import com.scrappy.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LedgerCodecTest {
    private val now=1_800_000_000_000L
    private fun draft()=LoadDraft(listOf(TripLedger.item("Copper #1",2.0,"kg",listOf(PriceQuote("Copper #1",4.25,"A | Yard\n東京",now))),
        TripLedger.item("Steel",10.0,"lb",emptyList())),TripCosts(40.0,18.0,3.6,90.0,20.0,4.0))
    @Test fun draftRoundTripRetainsSourcesNullDatesAndCosts() {
        val d=draft();assertEquals(d,LedgerCodec.decodeDraft(JSONObject(LedgerCodec.encodeDraft(d).toString())))
    }
    @Test fun multipleEstimatesRoundTripIndependently() {
        val e=LoadEstimate("e1","Load | one\n東京",now,draft())
        val e2=e.copy(id="e2",name="Second",draft=e.draft.copy(costs=TripCosts()))
        assertEquals(listOf(e,e2),LedgerCodec.decodeEstimates(LedgerCodec.encodeEstimates(listOf(e,e2))))
    }
    @Test fun saleRoundTripDoesNotDependOnOriginalEstimateStillExisting() {
        val e=LoadEstimate("e1","Load",now,draft());val s=TripLedger.receipt(e,"Yard",19.5,8.5,now,now=now)
        assertEquals(listOf(s),LedgerCodec.decodeSales(LedgerCodec.encodeSales(listOf(s))))
        assertEquals(11.0,s.cashNet,1e-9)
    }
    @Test fun corruptPayloadIsReportedNotSilentlyDiscarded() {
        assertTrue(runCatching { LedgerCodec.decodeEstimates("not-json") }.isFailure)
        assertTrue(runCatching { LedgerCodec.decodeSales("[{\"id\":\"x\"}]") }.isFailure)
    }
    @Test fun unknownSchemaIsRejected() {
        val o=LedgerCodec.encodeDraft(draft()).put("schema",2)
        assertTrue(runCatching { LedgerCodec.decodeDraft(o) }.isFailure)
    }
    @Test fun tamperedNonFiniteOrNegativeDataIsRejected() {
        val o=LedgerCodec.encodeDraft(draft());o.getJSONArray("items").getJSONObject(0).put("price",-1.0)
        assertTrue(runCatching { LedgerCodec.decodeDraft(o) }.isFailure)
    }
    @Test fun emptyCollectionsRoundTrip() {
        assertTrue(LedgerCodec.decodeEstimates("[]").isEmpty());assertTrue(LedgerCodec.decodeSales("[]").isEmpty())
        assertEquals(LoadDraft(),LedgerCodec.decodeDraft(LedgerCodec.encodeDraft(LoadDraft())))
    }
}
