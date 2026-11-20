package com.scrappy.app.data

import java.net.URI
import org.json.JSONObject

data class AiIdentification(val group: String, val confidence: String, val observations: List<String>,
    val checks: List<String>, val caution: String) {
    fun report() = "AI visual estimate\nLikely group: $group\nVisual confidence: $confidence (not independently verified)\n\n" +
        observations.joinToString("\n") + "\n\nConfirm with:\n" + checks.joinToString("\n") +
        "\n\n$caution\nA photo cannot verify alloy or scrap grade. Confirm with your receiving yard."
}
object AiIdentificationCodec {
    val groups = setOf("Copper", "Brass", "Aluminum", "Steel/Iron", "Stainless Steel", "Lead", "Zinc", "Unknown")
    fun endpoint(value: String): URI {
        val uri=URI(value)
        require(uri.scheme == "https" && uri.host != null && uri.userInfo == null &&
            uri.port in listOf(-1,443) && uri.rawQuery == null && uri.rawFragment == null && uri.path == "/v1/identify") {
            "AI needs a configured HTTPS service"
        }
        return uri
    }
    fun parse(raw: String): AiIdentification {
        require(raw.toByteArray(Charsets.UTF_8).size <= 16_384)
        val obj=JSONObject(raw)
        require(obj.keys().asSequence().toSet() == setOf("group","confidence","observations","checks","caution"))
        fun text(key: String, limit: Int): String {
            val value=obj.get(key);require(value is String && value.isNotBlank() && value.length <= limit)
            return value.trim()
        }
        fun list(key: String): List<String> {
            val array=obj.getJSONArray(key);require(array.length() in 1..5)
            return (0 until array.length()).map { i ->
                val value=array.get(i);require(value is String && value.isNotBlank() && value.length <= 300);value.trim()
            }
        }
        val group=text("group",30);require(group in groups)
        val confidence=text("confidence",10);require(confidence in setOf("low","medium","high"))
        return AiIdentification(group,confidence,list("observations"),list("checks"),text("caution",500))
    }
}
