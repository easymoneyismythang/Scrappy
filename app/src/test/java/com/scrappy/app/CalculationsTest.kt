package com.scrappy.app

import com.scrappy.app.data.Calculations
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationsTest {
    @Test fun laborCanMakeStrippingLessProfitable() {
        val r = Calculations.motor(100.0, 12.0, 0.28, 120.0, 20.0, 3.35, 0.09)
        assertEquals(28.0, r.whole, 0.0001)
        assertEquals(48.12, r.gross, 0.0001)
        assertEquals(8.12, r.net, 0.0001)
    }
    @Test fun zeroCopperUsesOnlySteelValue() {
        val r = Calculations.motor(10.0, 0.0, 0.28, 0.0, 20.0, 3.35, 0.09)
        assertEquals(0.9, r.net, 0.0001)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeWeight() {
        Calculations.motor(-1.0, 12.0, 0.28, 30.0, 20.0, 3.35, 0.09)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsImpossibleCopperPercentage() {
        Calculations.motor(10.0, 101.0, 0.28, 30.0, 20.0, 3.35, 0.09)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonFinitePrice() {
        Calculations.motor(10.0, 12.0, Double.NaN, 30.0, 20.0, 3.35, 0.09)
    }
}
