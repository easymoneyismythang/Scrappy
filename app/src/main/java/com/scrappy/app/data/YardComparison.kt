package com.scrappy.app.data

import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class YardTrip(val yard: String, val costs: TripCosts)
data class YardOption(val yard: String, val draft: LoadDraft?, val missingGrades: List<String>,
    val staleGrades: List<String>, val futureGrades: List<String>) {
    val totals: TripLedger.Totals? get() = draft?.let { TripLedger.totals(it) }
}
/** Uses only that yard's saved quotes. Never fills missing grades with demo prices. */
object YardComparison {
    private fun key(s: String) = s.trim().lowercase(Locale.ROOT)
    fun yards(quotes: List<PriceQuote>): List<String> = quotes
        .filter { runCatching { PriceQuotes.validate(it) }.isSuccess }
        .groupBy { key(it.yard) }.values.map { it.first().yard.trim() }.sortedBy { key(it) }

    fun compare(load: LoadDraft, quotes: List<PriceQuote>, trips: List<YardTrip>, now: Long): List<YardOption> {
        TripLedger.validate(load)
        require(load.items.isNotEmpty()) { "Add metal to the current load first" }
        require(now > 0)
        require(trips.isNotEmpty()) { "Add at least one yard to compare" }
        require(trips.size <= 20) { "Compare up to 20 yards at a time" }
        require(trips.map { key(it.yard) }.distinct().size == trips.size) { "Each yard can appear once" }
        val grades = load.items.map { it.metal }.distinct()
        return trips.map { trip ->
            require(trip.yard.isNotBlank() && trip.yard.length <= 120)
            TripLedger.validate(LoadDraft(costs = trip.costs))
            val selected = grades.associateWith { grade -> PriceQuotes.latest(grade,
                quotes.filter { key(it.yard) == key(trip.yard) }) }
            val missing = grades.filter { selected[it] == null }
            val stale = grades.filter { selected[it]?.let { q -> q.recordedAt <= now && now - q.recordedAt >= 86_400_000L } == true }
            val future = grades.filter { selected[it]?.recordedAt?.let { time -> time > now } == true }
            val draft = if (missing.isNotEmpty()) null else load.copy(costs = trip.costs,
                items = load.items.map { item ->
                    val q = selected.getValue(item.metal)!!
                    item.copy(pricePerLb = q.pricePerLb, source = trip.yard.trim(), quoteTime = q.recordedAt)
                }).also { TripLedger.validate(it) }
            YardOption(trip.yard.trim(), draft, missing, stale, future)
        }.sortedWith(compareByDescending<YardOption> { it.totals?.net ?: Double.NEGATIVE_INFINITY }
            .thenBy { key(it.yard) })
    }

    fun report(options: List<YardOption>): String {
        fun money(n: Double) = String.format(Locale.US, "$%.2f", n)
        val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
        val best = options.mapNotNull { it.totals?.net }.maxOrNull()
        return "Scrappy yard comparison\nSaved user quotes; estimated USD, not live offers.\n\n" +
            options.joinToString("\n\n") { o ->
                val t = o.totals
                o.yard + if (t == null) "\nIncomplete: missing quotes for ${o.missingGrades.joinToString()}. Not ranked." else
                    "\nGross: ${money(t.gross)} • Net after modeled costs: ${money(t.net)}" +
                    "\nFuel: ${money(t.fuel)} • Labor: ${money(t.labor)} • Fees: ${money(t.fees)}" +
                    "\nRound-trip miles: ${o.draft!!.costs.roundTripMiles} • Break-even payout: ${money(t.breakEvenPayout)}" +
                    "\nBehind highest estimated net: ${money(best!! - t.net)}" +
                    "\nAt least 24h old: ${o.staleGrades.joinToString().ifBlank { "None" }}" +
                    "\nFuture-dated: ${o.futureGrades.joinToString().ifBlank { "None" }}" +
                    "\n" + o.draft.items.joinToString("\n") { "${it.metal}: ${it.pounds} lb at ${String.format(Locale.US, "$%.4f", it.pricePerLb)}/lb (quote: ${date.format(Instant.ofEpochMilli(it.quoteTime!!))})" }
            } + "\n\nConfirm grades, quotes and yard acceptance before driving. Mileage and costs are your entries. Vehicle wear, taxes and unentered costs excluded. No ledger changes made."
    }
}
