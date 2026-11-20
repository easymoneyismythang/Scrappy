package com.scrappy.app.data

import org.json.JSONArray
import org.json.JSONObject

data class LedgerSnapshot(val draft: LoadDraft, val estimates: List<LoadEstimate>, val sales: List<SaleRecord>)

/** Portable ledger only: no photos, contacts, quotes or credentials. */
object LedgerBackup {
    const val MAX_BYTES = 8 * 1024 * 1024
    fun validate(s: LedgerSnapshot) {
        TripLedger.validate(s.draft)
        require(s.estimates.size <= TripLedger.MAX_RECORDS && s.sales.size <= TripLedger.MAX_RECORDS) { "Backup exceeds record capacity" }
        require(s.estimates.map { it.id }.distinct().size == s.estimates.size) { "Duplicate estimate IDs" }
        require(s.sales.map { it.id }.distinct().size == s.sales.size) { "Duplicate sale IDs" }
        s.estimates.forEach { TripLedger.validateEstimate(it) }
        // Sales remain valid after their originating estimate has been deleted.
        s.sales.forEach { TripLedger.validateSale(it) }
    }
    fun encode(s: LedgerSnapshot): String {
        validate(s)
        return JSONObject().put("format", "scrappy-ledger").put("version", 1)
            .put("draft", LedgerCodec.encodeDraft(s.draft))
            .put("estimates", JSONArray(LedgerCodec.encodeEstimates(s.estimates)))
            .put("sales", JSONArray(LedgerCodec.encodeSales(s.sales))).toString().also {
                require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is too large" }
            }
    }
    fun decode(raw: String): LedgerSnapshot {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is too large" }
        val o = JSONObject(raw)
        require(o.getString("format") == "scrappy-ledger" && o.get("version") == 1) { "Unsupported Scrappy backup" }
        val estimates = o.getJSONArray("estimates")
        val sales = o.getJSONArray("sales")
        require(estimates.length() <= TripLedger.MAX_RECORDS && sales.length() <= TripLedger.MAX_RECORDS) { "Backup exceeds record capacity" }
        require(o.getJSONObject("draft").getJSONArray("items").length() <= 100) { "Too many draft items" }
        return LedgerSnapshot(LedgerCodec.decodeDraft(o.getJSONObject("draft")),
            LedgerCodec.decodeEstimates(estimates.toString()), LedgerCodec.decodeSales(sales.toString())).also { validate(it) }
    }
}
