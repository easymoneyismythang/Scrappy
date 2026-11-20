package com.scrappy.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WeatherService {
    data class Place(val name: String, val latitude: Double, val longitude: Double)
    data class Conditions(val temperature: Double, val code: Int, val isDay: Boolean, val time: String)
    private fun get(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000; connection.readTimeout = 8_000
        try {
            check(connection.responseCode == 200) { "Weather service unavailable" }
            return connection.inputStream.use {
                JSONObject(String(BoundedResponse.read(it, 262_144), Charsets.UTF_8))
            }
        } finally { connection.disconnect() }
    }
    fun search(city: String): List<Place> {
        val results = get("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(city, "UTF-8")}&count=5&language=en&format=json").optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            Place(listOf(item.getString("name"),item.optString("admin1"),item.optString("country")).filter{it.isNotBlank()}.joinToString(", "), item.getDouble("latitude"), item.getDouble("longitude"))
        }
    }
    fun current(place: Place): Conditions {
        require(place.latitude.isFinite() && place.latitude in -90.0..90.0 && place.longitude.isFinite() && place.longitude in -180.0..180.0)
        val data = get("https://api.open-meteo.com/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}&current=temperature_2m,weather_code,is_day&temperature_unit=fahrenheit&timezone=auto").getJSONObject("current")
        val temperature = data.getDouble("temperature_2m")
        check(temperature.isFinite())
        return Conditions(temperature,data.getInt("weather_code"),data.getInt("is_day")==1,data.getString("time"))
    }
    fun label(code: Int, isDay: Boolean = true): String = when(code) {
        0 -> if(isDay) "☀️ Clear" else "🌙 Clear"
        1,2 -> "🌤 Partly cloudy"
        3 -> "☁️ Overcast"
        45,48 -> "🌫 Fog"
        51,53,55 -> "🌦 Drizzle"
        56,57,66,67 -> "🌧 Freezing rain"
        61,63,65,80,81,82 -> "🌧 Rain"
        71,73,75,77,85,86 -> "❄️ Snow"
        95,96,99 -> "⛈ Thunderstorm"
        else -> "🌡 Conditions unavailable"
    }
}
