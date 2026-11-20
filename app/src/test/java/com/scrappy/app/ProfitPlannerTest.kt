package com.scrappy.app

import com.scrappy.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProfitPlannerTest {
    private val metal=ScrappyRepository.metals.first().name
    private val draft=LoadDraft(listOf(LoadItem("a",metal,100.0,2.0,"Yard",1L)),TripCosts(40.0,20.0,3.5,60.0,20.0,3.0))
    @Test fun priceScenarioChangesNetAndKeepsOriginal() {
        val next=ProfitPlanner.scenario(draft,10.0,3.5,40.0)
        assertEquals(220.0,TripLedger.totals(next).gross,1e-8);assertEquals(2.0,draft.items.single().pricePerLb,0.0)
        assertEquals("Hypothetical scenario",next.items.single().source);assertNull(next.items.single().quoteTime)
    }
    @Test fun fuelScenario() { assertEquals(10.0,TripLedger.totals(ProfitPlanner.scenario(draft,0.0,5.0,40.0)).fuel,0.0) }
    @Test fun distanceScenario() { assertEquals(14.0,TripLedger.totals(ProfitPlanner.scenario(draft,0.0,3.5,80.0)).fuel,0.0) }
    @Test fun invalidScenarioRejected() {
        for(v in listOf(-101.0,101.0,Double.NaN)) try { ProfitPlanner.scenario(draft,v,3.5,40.0);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun targetNetRequiresAllCosts() { assertEquals(130.0,ProfitPlanner.targets(draft,100.0).requiredPayout,0.0) }
    @Test fun requiredAveragePrice() { assertEquals(1.3,ProfitPlanner.targets(draft,100.0).averagePrice!!,1e-8) }
    @Test fun requiredWeightAssumesFixedMix() { assertEquals(65.0,ProfitPlanner.targets(draft,100.0).requiredPounds!!,1e-8) }
    @Test fun maxRoundTripMiles() { assertEquals(77.0/(3.5/20.0),ProfitPlanner.targets(draft,100.0).maxRoundTripMiles!!,1e-8) }
    @Test fun infeasibleOrZeroFuelDistanceUnavailable() {
        assertNull(ProfitPlanner.targets(draft,1000.0).maxRoundTripMiles)
        assertNull(ProfitPlanner.targets(draft.copy(costs=draft.costs.copy(fuelPerGallon=0.0)),100.0).maxRoundTripMiles)
        assertNull(ProfitPlanner.targets(LoadDraft(),10.0).requiredPounds)
    }
    @Test fun capacityCanExceed100() {
        assertEquals(200.0,ProfitPlanner.capacity(draft,50.0),0.0)
        try { ProfitPlanner.capacity(draft,0.0);fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun gradeBreakdown() {
        val d=draft.copy(items=draft.items+draft.items.single().copy(id="b",pounds=10.0))
        val row=ProfitPlanner.metals(d).single()
        assertEquals(110.0,row.pounds,0.0);assertEquals(220.0,row.value,0.0)
    }
    @Test fun quoteReviewDistinguishesDemoOldAndFuture() {
        val d=draft.copy(items=listOf(draft.items.single(),draft.items.single().copy(id="b",quoteTime=null,source="Demo benchmark"),draft.items.single().copy(id="c",quoteTime=100_000_001L)))
        val q=ProfitPlanner.quotes(d,100_000_000L)
        assertEquals(1,q.demoItems);assertEquals(1,q.staleItems);assertEquals(1,q.futureItems);assertEquals(2,q.sources)
    }
}
