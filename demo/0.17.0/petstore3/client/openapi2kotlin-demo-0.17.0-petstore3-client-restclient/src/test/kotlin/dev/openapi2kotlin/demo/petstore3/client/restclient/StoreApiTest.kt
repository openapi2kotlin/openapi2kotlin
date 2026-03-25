package dev.openapi2kotlin.demo.petstore3.client.restclient

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.client.StoreApi
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.assertJsonEquals
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.objectMapper
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.order
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.assertEquals

@SpringBootTest
class StoreApiTest {
    @Autowired
    private lateinit var storeApi: StoreApi

    @Test
    fun `createOrder posts body and maps response dto`() {
        val requestBody = order(id = 1, petId = 10, quantity = 2, status = "placed", complete = false)
        val responseBody = requestBody.copy(status = "approved")

        server.stubFor(
            post(urlPathEqualTo("/store/order"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = storeApi.createOrder(requestBody)

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `deleteStore sends order id as path param`() {
        server.stubFor(
            delete(urlPathEqualTo("/store/order/11"))
                .willReturn(aResponse().withStatus(200)),
        )

        storeApi.deleteStore(11L)

        server.verify(deleteRequestedFor(urlPathEqualTo("/store/order/11")))
    }

    @Test
    fun `retrieveInventory maps response body and exposes headers`() {
        val responseBody = mapOf("available" to 3, "pending" to 1)

        server.stubFor(
            get(urlPathEqualTo("/store/inventory"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withHeader("X-Demo", "petstore3")
                        .withBody(objectMapper.writeValueAsString(responseBody)),
                ),
        )

        val result = storeApi.retrieveInventoryWithHttpInfo()

        assertEquals(200, result.statusCode.value())
        assertEquals("petstore3", result.headers.getFirst("X-Demo"))
        assertEquals(responseBody, result.body)
    }

    @Test
    fun `retrieveStore maps response dto`() {
        val responseBody = order(id = 12, petId = 22, quantity = 1, status = "placed", complete = true)

        server.stubFor(
            get(urlPathEqualTo("/store/order/12"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = storeApi.retrieveStore(12L)

        assertJsonEquals(responseBody, result)
    }

    private companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            registry.add("app.petstore3.url") { server.baseUrl() }
        }
    }
}
