package com.scrappy.app

import com.scrappy.app.data.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class LedgerBackupTest {
    private val draft = LoadDraft(listOf(LoadItem("item",ScrappyRepository.metals.first().name,10.0,2.5,"Yard",123L)),TripCosts(40.0,20.0,3.5,30.0,15.0,2.0))
    private val estimate = LoadEstimate("estimate","Weekend",123L,draft)
    private val sale = TripLedger.receipt(estimate,"Yard",30.0,7.0,456L,50.0)
    private val snapshot = LedgerSnapshot(draft,listOf(estimate),listOf(sale))
    private fun rejected(raw: String) { try { LedgerBackup.decode(raw); fail("Accepted invalid backup") } catch (_: IllegalArgumentException) {} }
    @Test fun completeRoundTrip() { assertEquals(snapshot,LedgerBackup.decode(LedgerBackup.encode(snapshot))) }
    @Test fun emptyRoundTrip() {
        val empty = LedgerSnapshot(LoadDraft(),emptyList(),emptyList())
        assertEquals(empty,LedgerBackup.decode(LedgerBackup.encode(empty)))
    }
    @Test fun independentReceiptSurvivesDeletedEstimate() {
        val s=snapshot.copy(estimates=emptyList())
        assertEquals(s,LedgerBackup.decode(LedgerBackup.encode(s)))
    }
    @Test fun rejectsWrongFormat() { rejected(JSONObject(LedgerBackup.encode(snapshot)).put("format","other").toString()) }
    @Test fun rejectsFutureVersion() { rejected(JSONObject(LedgerBackup.encode(snapshot)).put("version",2).toString()) }
    @Test fun rejectsFractionalVersion() { rejected(JSONObject(LedgerBackup.encode(snapshot)).put("version",1.5).toString()) }
    @Test fun rejectsDuplicateSales() {
        val o=JSONObject(LedgerBackup.encode(snapshot));val a=o.getJSONArray("sales");a.put(a.getJSONObject(0));rejected(o.toString())
    }
    @Test fun rejectsDuplicateEstimates() {
        val o=JSONObject(LedgerBackup.encode(snapshot));val a=o.getJSONArray("estimates");a.put(a.getJSONObject(0));rejected(o.toString())
    }
    @Test fun rejectsOverCapacity() {
        val o=JSONObject(LedgerBackup.encode(snapshot));val a=JSONArray()
        repeat(201) { a.put(o.getJSONArray("sales").getJSONObject(0)) };o.put("sales",a);rejected(o.toString())
    }
    @Test fun rejectsTooManyDraftItems() {
        val o=JSONObject(LedgerBackup.encode(snapshot));val a=JSONArray()
        repeat(101) { a.put(o.getJSONObject("draft").getJSONArray("items").getJSONObject(0)) }
        o.getJSONObject("draft").put("items",a);rejected(o.toString())
    }
    @Test fun rejectsNegativeMileage() {
        val o=JSONObject(LedgerBackup.encode(snapshot));o.getJSONObject("draft").getJSONObject("costs").put("miles",-1);rejected(o.toString())
    }
    @Test fun rejectsOversizedInput() { rejected(" ".repeat(LedgerBackup.MAX_BYTES+1)) }
    @Test fun corruptJsonFails() {
        try { LedgerBackup.decode("{broken");fail("Accepted corrupt JSON") } catch (_: org.json.JSONException) {}
    }
}
