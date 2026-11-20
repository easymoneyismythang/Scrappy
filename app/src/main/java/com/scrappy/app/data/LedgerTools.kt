package com.scrappy.app.data

import java.util.UUID

data class MergePreview(val snapshot: LedgerSnapshot, val addedEstimates: Int, val addedSales: Int,
    val skippedEstimates: Int, val skippedSales: Int)
object LedgerTools {
    fun rename(e: LoadEstimate, name: String): LoadEstimate = e.copy(name = name.trim()).also { TripLedger.validateEstimate(it) }
    fun duplicate(e: LoadEstimate, name: String, now: Long): LoadEstimate =
        e.copy(id = UUID.randomUUID().toString(), name = name.trim(), savedAt = now,
            draft = e.draft.copy(items = e.draft.items.map { it.copy(id = UUID.randomUUID().toString()) }))
            .also { TripLedger.validateEstimate(it) }
    fun correct(s: SaleRecord, yard: String, payout: Double, expenses: Double, miles: Double, date: Long, now: Long): SaleRecord {
        require(date <= now) { "Sale date cannot be in the future" }
        // Historical model cannot be recovered if a zero-mile sale is edited to positive miles.
        val fuel = if (miles == s.roundTripMiles) s.modeledFuel else if (s.roundTripMiles > 0) s.modeledFuel / s.roundTripMiles * miles else 0.0
        return s.copy(yard = yard.trim(), actualPayout = payout, cashExpenses = expenses,
            roundTripMiles = miles, soldAt = date, modeledFuel = fuel).also { TripLedger.validateSale(it) }
    }
    fun scale(d: LoadDraft, factor: Double): LoadDraft {
        require(factor.isFinite() && factor > 0 && factor <= 1000)
        return d.copy(items = d.items.map { it.copy(pounds = it.pounds * factor) }).also { TripLedger.validate(it) }
    }
    fun copyItem(d: LoadDraft, id: String): LoadDraft {
        val item = d.items.first { it.id == id }
        return d.copy(items = d.items + item.copy(id = UUID.randomUUID().toString())).also { TripLedger.validate(it) }
    }
    fun reprice(d: LoadDraft, quotes: List<PriceQuote>): LoadDraft {
        require(d.items.isNotEmpty()) { "Add metal to the load first" }
        val result = d.copy(items = d.items.map { i ->
            val q = PriceQuotes.latest(i.metal, quotes) ?: error("No saved quote for ${i.metal}. Add one in Scrap Price Guide.")
            i.copy(pricePerLb = q.pricePerLb, source = q.yard, quoteTime = q.recordedAt)
        })
        TripLedger.validate(result)
        return result
    }
    fun consolidate(d: LoadDraft): LoadDraft = d.copy(items = d.items
        .groupBy { listOf(it.metal, it.pricePerLb, it.source, it.quoteTime) }
        .values.map { rows -> rows.first().copy(pounds = rows.sumOf { it.pounds }) }).also { TripLedger.validate(it) }
    fun sorted(d: LoadDraft): LoadDraft = d.copy(items = d.items.sortedWith(compareBy<LoadItem> { it.metal }.thenBy { it.id }))
    fun clearCosts(d: LoadDraft): LoadDraft = d.copy(costs = TripCosts()).also { TripLedger.validate(it) }
    fun merge(current: LedgerSnapshot, incoming: LedgerSnapshot, useIncomingDraft: Boolean = false): MergePreview {
        LedgerBackup.validate(current); LedgerBackup.validate(incoming)
        fun <T> combine(a: List<T>, b: List<T>, id: (T) -> String): List<T> {
            val existing = a.associateBy(id)
            b.forEach { row -> require(existing[id(row)] == null || existing[id(row)] == row) {
                "Conflicting record ${id(row)}. Merge canceled; choose replacement only after exporting a backup."
            } }
            val result = a + b.filter { id(it) !in existing }
            require(result.size <= TripLedger.MAX_RECORDS) { "Merge exceeds 200-record capacity. Export and remove old records first." }
            return result
        }
        val estimates = combine(current.estimates, incoming.estimates) { it.id }
        val sales = combine(current.sales, incoming.sales) { it.id }
        val addedE = estimates.size - current.estimates.size
        val addedS = sales.size - current.sales.size
        val result = LedgerSnapshot(if (useIncomingDraft) incoming.draft else current.draft, estimates, sales)
        LedgerBackup.validate(result)
        return MergePreview(result, addedE, addedS, incoming.estimates.size - addedE, incoming.sales.size - addedS)
    }
    fun strictUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        .decode(java.nio.ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
    fun csvForSpreadsheet(sales: List<SaleRecord>): String = "\uFEFF" + TripLedger.salesCsv(sales)
}
