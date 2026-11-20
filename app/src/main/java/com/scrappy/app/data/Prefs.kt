package com.scrappy.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("scrappy_prefs", Context.MODE_PRIVATE)
    data class Contact(val id:String,val name:String,val phone:String,val address:String)
    fun contacts(): List<Contact> = try {
        val array=JSONArray(p.getString("contacts","[]"))
        (0 until array.length()).map { i -> val c=array.getJSONObject(i);Contact(c.getString("id"),c.getString("name"),c.getString("phone"),c.optString("address")) }
    } catch (_: org.json.JSONException) { emptyList() }
    private fun saveContacts(items:List<Contact>) {
        val array=JSONArray();items.forEach{c->array.put(JSONObject().put("id",c.id).put("name",c.name).put("phone",c.phone).put("address",c.address))}
        p.edit().putString("contacts",array.toString()).apply()
    }
    fun addContact(name:String,phone:String,address:String)=saveContacts(contacts()+Contact(UUID.randomUUID().toString(),name,phone,address))
    fun removeContact(id:String)=saveContacts(contacts().filter{it.id!=id})
    fun weatherPlace(): WeatherService.Place? = try {
        val raw=p.getString("weather_place",null)
        if(raw==null) null else JSONObject(raw).let{WeatherService.Place(it.getString("name"),it.getDouble("lat"),it.getDouble("lon"))}
    } catch (_: org.json.JSONException) { null }
    fun setWeatherPlace(place:WeatherService.Place) {
        p.edit().putString("weather_place",JSONObject().put("name",place.name).put("lat",place.latitude).put("lon",place.longitude).toString()).remove("weather_cache").apply()
    }
    fun clearWeatherPlace(){p.edit().remove("weather_place").remove("weather_cache").apply()}
    fun weatherCache(): String = p.getString("weather_cache","") ?: ""
    fun saveWeatherCache(value:String){p.edit().putString("weather_cache",value).apply()}
    fun priceQuotes(): List<PriceQuote> = try {
        val array = JSONArray(p.getString("price_quotes", "[]"))
        (0 until array.length()).mapNotNull { i ->
            runCatching {
                val q = array.getJSONObject(i)
                PriceQuote(q.getString("metal"), q.getDouble("price"), q.getString("yard"), q.getLong("time"))
                    .also { PriceQuotes.validate(it) }
            }.getOrNull()
        }
    } catch (_: org.json.JSONException) { emptyList() }
    fun savePriceQuote(quote: PriceQuote) {
        PriceQuotes.validate(quote)
        val values = PriceQuotes.upsert(priceQuotes(), quote)
        val array = JSONArray()
        values.forEach { q -> array.put(JSONObject().put("metal", q.metal).put("price", q.pricePerLb).put("yard", q.yard).put("time", q.recordedAt)) }
        p.edit().putString("price_quotes", array.toString()).apply()
    }
    fun removePriceQuote(metal: String, yard: String) {
        val array = JSONArray()
        PriceQuotes.remove(priceQuotes(), metal, yard).forEach { q ->
            array.put(JSONObject().put("metal", q.metal).put("price", q.pricePerLb).put("yard", q.yard).put("time", q.recordedAt))
        }
        p.edit().putString("price_quotes", array.toString()).apply()
    }
    fun favoriteYards(): Set<String> = p.getStringSet("favorite_yards", emptySet()) ?: emptySet()
    fun toggleFavorite(id: String): Boolean {
        val s = favoriteYards().toMutableSet()
        val now = if (s.contains(id)) { s.remove(id); false } else { s.add(id); true }
        p.edit().putStringSet("favorite_yards", s).apply(); return now
    }
    fun defaultMetal(): String = p.getString("default_metal", "Copper #1") ?: "Copper #1"
    fun setDefaultMetal(v:String) = p.edit().putString("default_metal", v).apply()
    fun laborRate(): Double = p.getFloat("labor_rate", 20f).toDouble()
    fun setLaborRate(v:Double) { require(v.isFinite() && v in 0.0..10_000.0);p.edit().putFloat("labor_rate",v.toFloat()).apply() }
    fun addPriceSnapshot(metal:String, price:Double, source:String = "Demo benchmark") {
        require(price.isFinite() && price >= 0)
        val items = (listOf(PriceSnapshot(metal,price,System.currentTimeMillis(),source)) + history()).take(40)
        val array=JSONArray()
        items.forEach { x -> array.put(JSONObject().put("metal",x.metal).put("price",x.price).put("time",x.recordedAt).put("source",x.source)) }
        p.edit().putString("history_v2",array.toString()).remove("history").apply()
    }
    fun history(): List<PriceSnapshot> {
        val stored=p.getString("history_v2",null)
        if(stored!=null) return try {
            val array=JSONArray(stored)
            (0 until array.length()).mapNotNull { i -> runCatching {
                val x=array.getJSONObject(i)
                PriceSnapshot(x.getString("metal"),x.getDouble("price"),x.getLong("time"),x.optString("source","Demo benchmark"))
            }.getOrNull() }
        } catch (_: org.json.JSONException) { emptyList() }
        return (p.getString("history","") ?: "").lines().mapNotNull { line ->
            val x=line.split("|"); if(x.size==3) PriceSnapshot(x[1],x[2].toDoubleOrNull()?:return@mapNotNull null,x[0].toLongOrNull()?:return@mapNotNull null) else null
        }
    }
}
