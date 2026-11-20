package com.scrappy.app
import com.scrappy.app.data.ScrappyRepository
import org.junit.Assert.*
import org.junit.Test
class RepositoryTest {
    @Test fun allBenchmarkPricesAreNonNegative(){ assertTrue(ScrappyRepository.metals.all{it.benchmarkPerLb >= 0.0}) }
    @Test fun allYardsHaveIds(){ assertEquals(ScrappyRepository.yards.size, ScrappyRepository.yards.map{it.id}.distinct().size) }
    @Test fun copperExists(){ assertTrue(ScrappyRepository.priceFor("Copper #1") > 0.0) }
}
