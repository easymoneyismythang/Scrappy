package com.scrappy.app.data

import org.json.JSONArray
import org.json.JSONObject

object LedgerCodec {
    fun encodeDraft(d: LoadDraft): JSONObject {
        TripLedger.validate(d)
        val items=JSONArray()
        d.items.forEach { i -> items.put(JSONObject().put("id",i.id).put("metal",i.metal).put("lb",i.pounds)
            .put("price",i.pricePerLb).put("source",i.source).put("quoteTime",i.quoteTime ?: JSONObject.NULL)) }
        val c=d.costs
        return JSONObject().put("schema",1).put("items",items).put("costs",JSONObject()
            .put("miles",c.roundTripMiles).put("mpg",c.mpg).put("fuel",c.fuelPerGallon)
            .put("minutes",c.travelMinutes).put("labor",c.laborPerHour).put("fees",c.fees))
    }
    fun decodeDraft(o: JSONObject): LoadDraft {
        require(o.getInt("schema")==1)
        val a=o.getJSONArray("items");val c=o.getJSONObject("costs")
        val items=(0 until a.length()).map { n -> val i=a.getJSONObject(n)
            LoadItem(i.getString("id"),i.getString("metal"),i.getDouble("lb"),i.getDouble("price"),
                i.getString("source"),if(i.isNull("quoteTime")) null else i.getLong("quoteTime")) }
        return LoadDraft(items,TripCosts(c.getDouble("miles"),c.getDouble("mpg"),c.getDouble("fuel"),
            c.getDouble("minutes"),c.getDouble("labor"),c.getDouble("fees"))).also { TripLedger.validate(it) }
    }
    fun encodeEstimates(values: List<LoadEstimate>): String {
        val a=JSONArray();values.forEach { e -> TripLedger.validateEstimate(e)
            a.put(JSONObject().put("id",e.id).put("name",e.name).put("time",e.savedAt).put("draft",encodeDraft(e.draft))) };return a.toString()
    }
    fun decodeEstimates(raw: String): List<LoadEstimate> {
        val a=JSONArray(raw)
        return (0 until a.length()).map { n -> val e=a.getJSONObject(n)
            LoadEstimate(e.getString("id"),e.getString("name"),e.getLong("time"),decodeDraft(e.getJSONObject("draft")))
                .also { TripLedger.validateEstimate(it) } }
    }
    fun encodeSales(values: List<SaleRecord>): String {
        val a=JSONArray();values.forEach { s -> TripLedger.validateSale(s)
            a.put(JSONObject().put("id",s.id).put("estimate",s.estimateId).put("yard",s.yard).put("time",s.soldAt)
                .put("payout",s.actualPayout).put("expenses",s.cashExpenses).put("gross",s.estimatedGross).put("labor",s.estimatedLabor).put("miles",s.roundTripMiles).put("fuel",s.modeledFuel)) };return a.toString()
    }
    fun decodeSales(raw: String): List<SaleRecord> {
        val a=JSONArray(raw)
        return (0 until a.length()).map { n -> val s=a.getJSONObject(n)
            SaleRecord(s.getString("id"),s.getString("estimate"),s.getString("yard"),s.getLong("time"),
                s.getDouble("payout"),s.getDouble("expenses"),s.getDouble("gross"),s.getDouble("labor"),s.optDouble("miles",0.0),s.optDouble("fuel",0.0))
                .also { TripLedger.validateSale(it) } }
    }
}
