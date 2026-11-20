package com.scrappy.app

import com.scrappy.app.data.WeatherService
import org.junit.Assert.*
import org.junit.Test

class WeatherServiceTest {
    @Test fun distinguishesDayAndNightClearSky() {
        assertTrue(WeatherService.label(0,true).contains("☀️"))
        assertTrue(WeatherService.label(0,false).contains("🌙"))
    }
    @Test fun mapsThunderstormAndSnow() {
        assertTrue(WeatherService.label(95).contains("Thunderstorm"))
        assertTrue(WeatherService.label(86).contains("Snow"))
    }
    @Test fun unknownCodesDoNotClaimClearWeather() {
        assertTrue(WeatherService.label(999).contains("unavailable"))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsCoordinatesBeforeNetworkRequest() {
        WeatherService.current(WeatherService.Place("Invalid",91.0,0.0))
    }
}
