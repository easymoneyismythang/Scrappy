package com.scrappy.app.data

object Calculations {
    data class MotorResult(val whole: Double, val gross: Double, val labor: Double) {
        val net: Double get() = gross - labor
    }
    fun motor(weight: Double, copperPercent: Double, wholePrice: Double, minutes: Double,
              laborRate: Double, copperPrice: Double, steelPrice: Double): MotorResult {
        require(listOf(weight, wholePrice, minutes, laborRate, copperPrice, steelPrice).all { it.isFinite() && it >= 0.0 })
        require(copperPercent.isFinite() && copperPercent in 0.0..100.0)
        val copperWeight = weight * copperPercent / 100.0
        return MotorResult(weight * wholePrice, copperWeight * copperPrice + (weight - copperWeight) * steelPrice, minutes / 60.0 * laborRate)
    }
}
