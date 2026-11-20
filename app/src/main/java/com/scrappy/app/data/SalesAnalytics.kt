package com.scrappy.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class SaleSort { NEWEST, OLDEST, HIGHEST_NET, HIGHEST_PAYOUT }
enum class SalePeriod { WEEK, MONTH, YEAR }
data class SaleFilter(val start: LocalDate? = null, val end: LocalDate? = null,
    val yard: String? = null, val query: String = "", val sort: SaleSort = SaleSort.NEWEST)
data class Earnings(val count: Int, val payout: Double, val expenses: Double, val net: Double,
    val afterLabor: Double, val miles: Double, val fuel: Double, val expected: Double) {
    val averagePayout: Double? get() = if (count == 0) null else payout / count
    val averageNet: Double? get() = if (count == 0) null else net / count
    val netPerMile: Double? get() = if (miles == 0.0) null else net / miles
    val expensePerMile: Double? get() = if (miles == 0.0) null else expenses / miles
    val payoutDifference: Double get() = payout - expected
    val payoutDifferencePercent: Double? get() = if (expected == 0.0) null else payoutDifference / expected * 100
}
data class EarningsGroup(val label: String, val totals: Earnings)
object SalesAnalytics {
    fun localDate(s: SaleRecord, zone: ZoneId): LocalDate = Instant.ofEpochMilli(s.soldAt).atZone(zone).toLocalDate()
    fun filter(sales: List<SaleRecord>, f: SaleFilter, zone: ZoneId): List<SaleRecord> {
        require(f.start == null || f.end == null || !f.start.isAfter(f.end)) { "Start date must be before end date" }
        sales.forEach { TripLedger.validateSale(it) }
        val query = f.query.trim().lowercase(Locale.ROOT)
        val selected = sales.filter { s ->
            val date = localDate(s, zone)
            (f.start == null || !date.isBefore(f.start)) && (f.end == null || !date.isAfter(f.end)) &&
                (f.yard == null || s.yard.trim().equals(f.yard.trim(), true)) &&
                (query.isEmpty() || listOf(s.yard, s.id, s.estimateId).any { it.lowercase(Locale.ROOT).contains(query) })
        }
        return when (f.sort) {
            SaleSort.NEWEST -> selected.sortedWith(compareByDescending<SaleRecord> { it.soldAt }.thenBy { it.id })
            SaleSort.OLDEST -> selected.sortedWith(compareBy<SaleRecord> { it.soldAt }.thenBy { it.id })
            SaleSort.HIGHEST_NET -> selected.sortedWith(compareByDescending<SaleRecord> { it.cashNet }.thenBy { it.id })
            SaleSort.HIGHEST_PAYOUT -> selected.sortedWith(compareByDescending<SaleRecord> { it.actualPayout }.thenBy { it.id })
        }
    }
    fun totals(sales: List<SaleRecord>): Earnings {
        sales.forEach { TripLedger.validateSale(it) }
        return Earnings(sales.size, sales.sumOf { it.actualPayout }, sales.sumOf { it.cashExpenses },
            sales.sumOf { it.cashNet }, sales.sumOf { it.afterLabor }, sales.sumOf { it.roundTripMiles },
            sales.sumOf { it.modeledFuel }, sales.sumOf { it.estimatedGross })
    }
    fun periods(sales: List<SaleRecord>, period: SalePeriod, zone: ZoneId): List<EarningsGroup> =
        sales.groupBy { s ->
            val d = localDate(s, zone)
            when (period) {
                SalePeriod.WEEK -> d.minusDays((d.dayOfWeek.value - 1).toLong()).toString()
                SalePeriod.MONTH -> d.toString().take(7)
                SalePeriod.YEAR -> d.year.toString()
            }
        }.toSortedMap(compareByDescending { it }).map { (label, rows) -> EarningsGroup(label, totals(rows)) }
    fun yards(sales: List<SaleRecord>): List<EarningsGroup> = sales.groupBy { it.yard.trim().lowercase(Locale.ROOT) }
        .map { (_, rows) -> EarningsGroup(rows.first().yard.trim(), totals(rows)) }
        .sortedWith(compareByDescending<EarningsGroup> { it.totals.net }.thenBy { it.label })
}
