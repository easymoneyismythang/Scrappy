package com.scrappy.app.data

import android.content.Context
import org.json.JSONObject

class LedgerStore(context: Context) {
    private val prefs=context.getSharedPreferences("scrappy_ledger",Context.MODE_PRIVATE)
    // Read failures are surfaced. Corrupt data is never silently replaced with an empty ledger.
    fun snapshot() = LedgerSnapshot(draft(), estimates(), sales())
    fun restore(snapshot: LedgerSnapshot, keepCheckpoint: Boolean = false) {
        LedgerBackup.validate(snapshot)
        val draftJson = LedgerCodec.encodeDraft(snapshot.draft).toString()
        val estimatesJson = LedgerCodec.encodeEstimates(snapshot.estimates)
        val salesJson = LedgerCodec.encodeSales(snapshot.sales)
        val editor = prefs.edit().putString("draft",draftJson).putString("estimates",estimatesJson)
            .putString("sales",salesJson).remove("undo")
        if (keepCheckpoint) editor.putString("beforeImport", LedgerBackup.encode(this.snapshot()))
        check(editor.commit()) { "Could not restore on this device" }
    }
    fun draft(): LoadDraft = prefs.getString("draft",null)?.let { LedgerCodec.decodeDraft(JSONObject(it)) } ?: LoadDraft()
    fun estimates(): List<LoadEstimate> = LedgerCodec.decodeEstimates(prefs.getString("estimates","[]") ?: "[]")
    fun sales(): List<SaleRecord> = LedgerCodec.decodeSales(prefs.getString("sales","[]") ?: "[]")
    private fun write(key: String, value: String) { check(prefs.edit().putString(key,value).remove("undo").commit()) { "Could not save on this device" } }
    fun saveDraft(d: LoadDraft) = write("draft",LedgerCodec.encodeDraft(d).toString())
    fun saveEstimate(e: LoadEstimate) {
        val values=estimates();require(values.size < TripLedger.MAX_RECORDS) { "200 estimates saved. Remove an old estimate first." }
        require(values.none { it.id==e.id });write("estimates",LedgerCodec.encodeEstimates(listOf(e)+values))
    }
    fun removeEstimate(id: String) {
        val values = estimates(); require(values.any { it.id == id })
        deleteWithUndo("estimates", LedgerCodec.encodeEstimates(values.filter { it.id != id }))
    }
    fun saveSale(s: SaleRecord) {
        val values=sales();require(values.size < TripLedger.MAX_RECORDS) { "200 sales saved. Export and remove old records first." }
        require(values.none { it.id==s.id });write("sales",LedgerCodec.encodeSales(listOf(s)+values))
    }
    fun removeSale(id: String) {
        val values = sales(); require(values.any { it.id == id })
        deleteWithUndo("sales", LedgerCodec.encodeSales(values.filter { it.id != id }))
    }
    private fun deleteWithUndo(key: String, value: String) {
        val before = LedgerBackup.encode(snapshot())
        check(prefs.edit().putString(key,value).putString("undo",before).commit()) { "Could not delete on this device" }
    }
    fun canUndo() = prefs.contains("undo")
    fun undoDelete() {
        val raw = prefs.getString("undo",null) ?: error("No deletion to undo")
        restore(LedgerBackup.decode(raw))
    }
    fun hasImportCheckpoint() = prefs.contains("beforeImport")
    fun recoverBeforeImport() {
        val raw = prefs.getString("beforeImport",null) ?: error("No pre-import checkpoint")
        val previous = LedgerBackup.decode(raw)
        // Current ledger becomes the checkpoint, allowing reversal of recovery too.
        restore(previous, keepCheckpoint = true)
    }
    fun updateEstimate(e: LoadEstimate) {
        TripLedger.validateEstimate(e)
        val values = estimates(); require(values.any { it.id == e.id }) { "Estimate no longer exists" }
        write("estimates", LedgerCodec.encodeEstimates(values.map { if(it.id == e.id) e else it }))
    }
    fun updateSale(s: SaleRecord) {
        TripLedger.validateSale(s)
        val values = sales(); require(values.any { it.id == s.id }) { "Sale no longer exists" }
        write("sales", LedgerCodec.encodeSales(values.map { if(it.id == s.id) s else it }))
    }
}
