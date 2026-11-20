package com.scrappy.app.data

data class Metal(
    val name: String,
    val category: String,
    val benchmarkPerLb: Double,
    val magnetic: Boolean,
    val colorHint: String,
    val notes: String
)

data class Yard(
    val id: String,
    val name: String,
    val miles: Double,
    val phone: String,
    val site: String,
    val address: String,
    val prices: Map<String, Double>,
    val hours: String = "Call to confirm hours"
)

data class PriceSnapshot(val metal: String, val price: Double, val recordedAt: Long, val source: String = "Demo benchmark")
