package com.scrappy.app.data

data class ProfitTargets(val requiredPayout: Double, val averagePrice: Double?,
    val requiredPounds: Double?, val maxRoundTripMiles: Double?)
data class MetalSubtotal(val metal: String, val pounds: Double, val value: Double)
data class QuoteReview(val demoItems: Int, val staleItems: Int, val futureItems: Int, val sources: Int)
object ProfitPlanner {
    fun scenario(d: LoadDraft, priceChangePercent: Double, fuelPrice: Double, roundTripMiles: Double): LoadDraft {
        TripLedger.validate(d)
        require(priceChangePercent.isFinite() && priceChangePercent in -100.0..100.0)
        val result = d.copy(items = d.items.map { it.copy(pricePerLb = it.pricePerLb * (1 + priceChangePercent / 100),
            source = "Hypothetical scenario", quoteTime = null) },
            costs = d.costs.copy(fuelPerGallon = fuelPrice, roundTripMiles = roundTripMiles))
        TripLedger.validate(result)
        return result
    }
    // Other costs held fixed. These are planning relationships, not predictions.
    fun targets(d: LoadDraft, targetNet: Double): ProfitTargets {
        require(targetNet.isFinite() && targetNet in 0.0..1_000_000_000_000.0)
        val t = TripLedger.totals(d)
        val required = targetNet + t.breakEvenPayout
        val price = if (t.pounds == 0.0) null else t.gross / t.pounds
        val fuelPerMile = d.costs.fuelPerGallon / d.costs.mpg
        val available = t.gross - t.labor - t.fees - targetNet
        return ProfitTargets(required, if (t.pounds == 0.0) null else required / t.pounds,
            if (price == null || price == 0.0) null else required / price,
            if (fuelPerMile == 0.0 || available < 0) null else available / fuelPerMile)
    }
    fun capacity(d: LoadDraft, capacityPounds: Double): Double {
        require(capacityPounds.isFinite() && capacityPounds > 0 && capacityPounds <= 10_000_000)
        return TripLedger.totals(d).pounds / capacityPounds * 100
    }
    fun metals(d: LoadDraft): List<MetalSubtotal> {
        TripLedger.validate(d)
        return d.items.groupBy { it.metal }.map { (metal, items) ->
            MetalSubtotal(metal, items.sumOf { it.pounds }, items.sumOf { it.value })
        }.sortedByDescending { it.value }
    }
    fun quotes(d: LoadDraft, now: Long): QuoteReview {
        TripLedger.validate(d)
        return QuoteReview(d.items.count { it.quoteTime == null },
            d.items.count { it.quoteTime != null && it.quoteTime <= now && now - it.quoteTime >= 86_400_000L },
            d.items.count { it.quoteTime != null && it.quoteTime > now }, d.items.map { it.source }.distinct().size)
    }
}
