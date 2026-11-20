package com.scrappy.app.data

data class PriceQuote(val metal: String, val pricePerLb: Double, val yard: String, val recordedAt: Long)

object PriceQuotes {
    private fun yardKey(value: String) = value.trim().lowercase(java.util.Locale.ROOT)
    fun upsert(quotes: List<PriceQuote>, quote: PriceQuote): List<PriceQuote> {
        validate(quote)
        val kept=quotes.filterNot { it.metal == quote.metal && yardKey(it.yard) == yardKey(quote.yard) }
        require(kept.size < 500) { "Quote storage is full. Remove an old quote before adding another." }
        return kept + quote.copy(yard=quote.yard.trim())
    }
    fun remove(quotes: List<PriceQuote>, metal: String, yard: String): List<PriceQuote> =
        quotes.filterNot { it.metal == metal && yardKey(it.yard) == yardKey(yard) }

    private const val DAY = 86_400_000L
    fun validate(quote: PriceQuote) {
        require(ScrappyRepository.metals.any { it.name == quote.metal })
        require(quote.pricePerLb.isFinite() && quote.pricePerLb in 0.0..10_000.0)
        require(quote.yard.isNotBlank() && quote.yard.length <= 120)
        require(quote.recordedAt > 0)
    }
    fun latest(metal: String, quotes: List<PriceQuote>): PriceQuote? = quotes
        .filter { it.metal == metal && runCatching { validate(it) }.isSuccess }
        .maxByOrNull { it.recordedAt }

    fun needsConfirmation(quote: PriceQuote, now: Long): Boolean =
        quote.recordedAt > now || now - quote.recordedAt >= DAY

    /** A saved yard quote only. Null means the app has no user-entered market quote for this grade. */
    fun confirmedPriceFor(metal: String, quotes: List<PriceQuote>): Double? =
        latest(metal, quotes)?.pricePerLb

    /** Explicit estimate helper: falls back to Scrappy's built-in demo benchmark when no saved quote exists. */
    fun estimatePriceFor(metal: String, quotes: List<PriceQuote>): Double =
        confirmedPriceFor(metal, quotes) ?: ScrappyRepository.priceFor(metal)

    @Deprecated("Use confirmedPriceFor for market quotes or estimatePriceFor for labeled estimates")
    fun priceFor(metal: String, quotes: List<PriceQuote>): Double = estimatePriceFor(metal, quotes)
}
