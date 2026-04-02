package dev.openapi2kotlin.demo.petstore3.client.http4k

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.client.StoreApi
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.json
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.order
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreApiTest : AbstractApiTest() {
    private val storeApi: StoreApi
        get() = configuration.storeApi()

    @Test
    fun `createOrder posts body and maps response dto`() {
        val requestBody = order(id = 1, petId = 10, quantity = 2, status = "placed", complete = false)
        val responseBody = requestBody.copy(status = "approved")

        server.stubFor(
            post(urlPathEqualTo("/store/order"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = storeApi.createOrder(requestBody)

        assertEquals(responseBody, result)
    }

    @Test
    fun `deleteStore sends order id as path param`() {
        server.stubFor(
            delete(urlPathEqualTo("/store/order/11"))
                .willReturn(aResponse().withStatus(200)),
        )

        val result = storeApi.deleteStoreWithHttpInfo(11L)

        assertEquals(200, result.status.code)
        assertEquals("", result.bodyString())
    }

    @Test
    fun `retrieveInventory maps response body and exposes headers`() {
        server.stubFor(
            get(urlPathEqualTo("/store/inventory"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Demo", "petstore3")
                        .withBody("""{"available":3,"pending":1}"""),
                ),
        )

        val result = storeApi.retrieveInventoryWithHttpInfo()

        assertEquals(200, result.status.code)
        assertEquals("petstore3", result.header("X-Demo"))
        assertEquals(3, json.decodeFromString<JsonObject>(result.bodyString())["available"]?.toString()?.toInt())
    }

    @Test
    fun `retrieveStore maps response dto`() {
        val responseBody = order(id = 12, petId = 22, quantity = 1, status = "placed", complete = true)

        server.stubFor(
            get(urlPathEqualTo("/store/order/12"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = storeApi.retrieveStore(12L)

        assertEquals(responseBody, result)
    }
}
