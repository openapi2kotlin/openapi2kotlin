package dev.openapi2kotlin.demo.petstore3.client.ktor

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.order
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreApiTest : AbstractApiTest() {
    @Test
    fun `createOrder posts body and maps response dto`() =
        withApiTest {
            val storeApi = resolveStoreApi()
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
    fun `deleteStore sends order id as path param`() =
        withApiTest {
            val storeApi = resolveStoreApi()
            server.stubFor(
                delete(urlPathEqualTo("/store/order/11"))
                    .willReturn(aResponse().withStatus(200)),
            )

            storeApi.deleteStore(11L)

            server.verify(deleteRequestedFor(urlPathEqualTo("/store/order/11")))
        }

    @Test
    fun `retrieveInventory maps response body`() =
        withApiTest {
            val storeApi = resolveStoreApi()
            val responseBody = Json.parseToJsonElement("{\"available\":3,\"pending\":1}")

            server.stubFor(
                get(urlPathEqualTo("/store/inventory"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"available\":3,\"pending\":1}"),
                    ),
            )

            val result = storeApi.retrieveInventory()

            assertEquals(responseBody, result)
        }

    @Test
    fun `retrieveStore maps response dto`() =
        withApiTest {
            val storeApi = resolveStoreApi()
            val responseBody = order(id = 12, petId = 22, quantity = 1, status = "placed", complete = true)

            server.stubFor(
                get(urlPathEqualTo("/store/order/12"))
                    .willReturn(jsonResponse(responseBody)),
            )

            val result = storeApi.retrieveStore(12L)

            assertEquals(responseBody, result)
        }
}
