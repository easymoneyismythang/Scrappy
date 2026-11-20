package com.scrappy.app.data

import java.util.UUID

// All values are USD and pounds internally. Quotes are snapshots, never repriced silently.
data class LoadItem(val id: String, val metal: String, val pounds: Double, val pricePerLb: Double,
                    val source: String, val quoteTime: Long?) {
    val value: Double get() = pounds * pricePerLb
}
data class TripCosts(val roundTripMiles: Double = 0.0, val mpg: Double = 20.0,
    val fuelPerGallon: Double = 0.0, val travelMinutes: Double = 0.0,
    val laborPerHour: Double = 0.0, val fees: Double = 0.0)
data class LoadDraft(val items: List<LoadItem> = emptyList(), val costs: TripCosts = TripCosts())
data class LoadEstimate(val id: String, val name: String, val savedAt: Long, val draft: LoadDraft)
data class SaleRecord(val id: String, val estimateId: String, val yard: String, val soldAt: Long,
    val actualPayout: Double, val cashExpenses: Double, val estimatedGross: Double,
    val estimatedLabor: Double, val roundTripMiles: Double = 0.0, val modeledFuel: Double = 0.0) {
    val cashNet: Double get() = actualPayout - cashExpenses
    val afterLabor: Double get() = cashNet - estimatedLabor
    val payoutDifference: Double get() = actualPayout - estimatedGross
}
object TripLedger {
    const val LB_PER_KG = 2.2046226218487757
    const val MAX_RECORDS = 200
    fun pounds(weight: Double, unit: String): Double {
        require(weight.isFinite() && weight > 0 && weight <= 1_000_000)
        require(unit == "lb" || unit == "kg")
        return if (unit == "kg") weight * LB_PER_KG else weight
    }
    fun item(metal: String, weight: Double, unit: String, quotes: List<PriceQuote>): LoadItem {
        require(ScrappyRepository.metals.any { it.name == metal })
        val q = PriceQuotes.latest(metal, quotes)
        return LoadItem(UUID.randomUUID().toString(), metal, pounds(weight, unit),
            q?.pricePerLb ?: ScrappyRepository.priceFor(metal), q?.yard ?: "Demo benchmark", q?.recordedAt)
    }
    fun validate(draft: LoadDraft) {
        require(draft.items.size <= 100)
        require(draft.items.map { it.id }.distinct().size == draft.items.size)
        draft.items.forEach {
            require(it.id.isNotBlank() && ScrappyRepository.metals.any { m -> m.name == it.metal })
            require(it.pounds.isFinite() && it.pounds > 0 && it.pounds <= 1_000_000 * LB_PER_KG)
            require(it.pricePerLb.isFinite() && it.pricePerLb in 0.0..10_000.0)
            require(it.source.isNotBlank() && it.source.length <= 120)
            require(it.quoteTime == null || it.quoteTime > 0)
        }
        val c = draft.costs
        require(listOf(c.roundTripMiles, c.fuelPerGallon, c.travelMinutes, c.laborPerHour, c.fees)
            .all { it.isFinite() && it in 0.0..1_000_000.0 })
        require(c.mpg.isFinite() && c.mpg > 0 && c.mpg <= 1_000)
    }
    data class Totals(val pounds: Double, val gross: Double, val fuel: Double, val labor: Double, val fees: Double) {
        val cashCosts get() = fuel + fees
        val net get() = gross - cashCosts - labor
        val breakEvenPayout get() = cashCosts + labor
    }
    fun totals(draft: LoadDraft): Totals {
        validate(draft)
        val c = draft.costs
        return Totals(draft.items.sumOf { it.pounds }, draft.items.sumOf { it.value },
            c.roundTripMiles / c.mpg * c.fuelPerGallon, c.travelMinutes / 60 * c.laborPerHour, c.fees)
    }
    fun validateEstimate(e: LoadEstimate) {
        require(e.id.isNotBlank() && e.name.isNotBlank() && e.name.length <= 120 && e.savedAt > 0)
        require(e.draft.items.isNotEmpty()); validate(e.draft)
    }
    fun validateSale(s: SaleRecord) {
        require(s.id.isNotBlank() && s.estimateId.isNotBlank() && s.yard.isNotBlank() && s.yard.length <= 120 && s.soldAt > 0)
        require(listOf(s.actualPayout,s.cashExpenses,s.estimatedGross,s.estimatedLabor,s.modeledFuel).all { it.isFinite() && it in 0.0..1_000_000_000_000_000.0 })
        require(s.roundTripMiles.isFinite() && s.roundTripMiles in 0.0..1_000_000.0)
    }
    fun receipt(e: LoadEstimate, yard: String, payout: Double, expenses: Double, date: Long, actualMiles: Double = e.draft.costs.roundTripMiles, now: Long = System.currentTimeMillis()): SaleRecord {
        validateEstimate(e)
        require(date <= now) { "Sale date cannot be in the future" }
        return SaleRecord(UUID.randomUUID().toString(),e.id,yard.trim(),date,payout,expenses,
            totals(e.draft).gross,totals(e.draft).labor,actualMiles,
            actualMiles / e.draft.costs.mpg * e.draft.costs.fuelPerGallon).also { validateSale(it) }
    }
    // CSV quoting also neutralizes spreadsheet formula prefixes in user-entered strings.
    private fun cell(s: String): String {
        val safe = if (s.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || s.startsWith("\t") || s.startsWith("\r")) "'" + s else s
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
    fun salesCsv(sales: List<SaleRecord>): String {
        sales.forEach { validateSale(it) }
        val header = "id,estimate_id,yard,sold_at_epoch_ms,payout_usd,cash_expenses_usd,cash_net_usd,estimated_labor_usd,after_labor_usd,estimated_gross_usd,payout_difference_usd,round_trip_miles,modeled_fuel_usd"
        return header + "\r\n" + sales.joinToString("\r\n") { s ->
            listOf(cell(s.id),cell(s.estimateId),cell(s.yard),s.soldAt.toString(),s.actualPayout.toString(),
                s.cashExpenses.toString(),s.cashNet.toString(),s.estimatedLabor.toString(),s.afterLabor.toString(),
                s.estimatedGross.toString(),s.payoutDifference.toString(),s.roundTripMiles.toString(),s.modeledFuel.toString()).joinToString(",")
        } + "\r\n"
    }
}
