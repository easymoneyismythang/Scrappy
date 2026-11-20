package com.scrappy.app

import com.scrappy.app.data.PriceQuote
import com.scrappy.app.data.PriceQuotes
import com.scrappy.app.data.ScrappyRepository
import org.junit.Assert.*
import org.junit.Test

class PriceQuotesTest {
    private val now = 1_800_000_000_000L
    private fun quote(price: Double = 4.0, time: Long = now) = PriceQuote("Copper #2", price, "My yard", time)

    @Test fun quoteOverridesDemoOnlyForItsMetal() {
        assertEquals(4.0, PriceQuotes.estimatePriceFor("Copper #2", listOf(quote())), 0.00001)
        assertEquals(ScrappyRepository.priceFor("Steel"), PriceQuotes.estimatePriceFor("Steel", listOf(quote())), 0.00001)
    }
    @Test fun newestQuoteWinsRegardlessOfListOrder() {
        assertEquals(4.0, PriceQuotes.estimatePriceFor("Copper #2", listOf(quote(), quote(3.0, now - 1))), 0.00001)
    }
    @Test fun zeroQuoteDoesNotFallBackToDemo() {
        assertEquals(0.0, PriceQuotes.estimatePriceFor("Copper #2", listOf(quote(0.0))), 0.00001)
    }
    @Test fun invalidStoredQuoteCannotOverrideValidQuote() {
        assertEquals(4.0, PriceQuotes.estimatePriceFor("Copper #2", listOf(quote(), quote(Double.NaN, now + 1))), 0.00001)
    }
    @Test fun confirmationStartsAtExactly24Hours() {
        assertFalse(PriceQuotes.needsConfirmation(quote(), now + 86_399_999))
        assertTrue(PriceQuotes.needsConfirmation(quote(), now + 86_400_000))
        assertTrue(PriceQuotes.needsConfirmation(quote(), now - 1))
    }
    @Test fun invalidQuotesAreRejected() {
        listOf(quote(-1.0), quote(Double.POSITIVE_INFINITY), quote(10_001.0), quote().copy(metal="Unknown"),
            quote().copy(yard=" "), quote().copy(yard="x".repeat(121)), quote(time=0)).forEach {
            assertTrue(runCatching { PriceQuotes.validate(it) }.isFailure)
        }
    }
    @Test fun removedQuoteReturnsToDemo() {
        assertEquals(ScrappyRepository.priceFor("Copper #2"), PriceQuotes.estimatePriceFor("Copper #2", emptyList()), 0.00001)
    }

    @Test fun confirmedPriceNeverReturnsDemoBenchmark() {
        assertNull(PriceQuotes.confirmedPriceFor("Copper #2", emptyList()))
        assertEquals(4.0, PriceQuotes.confirmedPriceFor("Copper #2", listOf(quote()))!!, 0.00001)
    }

    @Test fun savingSecondYardRetainsFirst() {
        val values=PriceQuotes.upsert(listOf(quote()),quote(5.0).copy(yard="Other"))
        assertEquals(2,values.size)
    }
    @Test fun updatingYardNormalizesNameAndRetainsOtherGrades() {
        val values=PriceQuotes.upsert(listOf(quote(),quote().copy(metal="Steel")),quote(5.0).copy(yard=" MY YARD "))
        assertEquals(2,values.size);assertEquals(5.0,values.last().pricePerLb,0.0)
    }
    @Test fun removalOnlyRemovesSpecifiedGradeAndYard() {
        val values=listOf(quote(),quote().copy(yard="Other"),quote().copy(metal="Steel"))
        assertEquals(2,PriceQuotes.remove(values,"Copper #2"," my yard ").size)
    }
    @Test fun quoteCapacityRejectsOverflowButAllowsUpdate() {
        val values=(1..500).map { quote().copy(yard="Yard $it") }
        assertEquals(500,PriceQuotes.upsert(values,quote(5.0).copy(yard="Yard 1")).size)
        assertTrue(runCatching { PriceQuotes.upsert(values,quote()) }.isFailure)
    }
}
