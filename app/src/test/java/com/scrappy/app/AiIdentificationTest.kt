package com.scrappy.app

import com.scrappy.app.data.AiIdentificationCodec
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class AiIdentificationTest {
    private val raw="""{"group":"Copper","confidence":"low","observations":["Reddish surface"],"checks":["Confirm with the yard"],"caution":"Photo cannot prove alloy."}"""
    @Test fun validResultIncludesUncertainty() { assertTrue(AiIdentificationCodec.parse(raw).report().contains("cannot verify alloy")) }
    @Test fun unsupportedGradeRejected() { assertTrue(runCatching { AiIdentificationCodec.parse(raw.replace("Copper","Gold")) }.isFailure) }
    @Test fun missingFieldRejected() { val o=JSONObject(raw);o.remove("checks");assertTrue(runCatching { AiIdentificationCodec.parse(o.toString()) }.isFailure) }
    @Test fun extraFieldsRejected() { assertTrue(runCatching { AiIdentificationCodec.parse(JSONObject(raw).put("price",4.0).toString()) }.isFailure) }
    @Test fun emptyOrOversizedChecksRejected() {
        assertTrue(runCatching { AiIdentificationCodec.parse(raw.replace("[\"Confirm with the yard\"]","[]")) }.isFailure)
        assertTrue(runCatching { AiIdentificationCodec.parse(raw.replace("Confirm with the yard","x".repeat(301))) }.isFailure)
    }
    @Test fun nonStringAndInvalidConfidenceRejected() {
        assertTrue(runCatching { AiIdentificationCodec.parse(JSONObject(raw).put("caution",42).toString()) }.isFailure)
        assertTrue(runCatching { AiIdentificationCodec.parse(raw.replace("low","certain")) }.isFailure)
    }
    @Test fun oversizedBodyRejected() { assertTrue(runCatching { AiIdentificationCodec.parse(" ".repeat(16_385)) }.isFailure) }
    @Test fun httpsEndpointAccepted() { assertEquals("service.example",AiIdentificationCodec.endpoint("https://service.example/v1/identify").host) }
    @Test fun httpCredentialsQueriesAndOtherPathsRejected() {
        for(url in listOf("http://service.example/v1/identify","https://key@service.example/v1/identify",
            "https://service.example/v1/identify?key=private","https://service.example/other","https://service.example:8443/v1/identify","")) {
            assertTrue(runCatching { AiIdentificationCodec.endpoint(url) }.isFailure)
        }
    }
}
