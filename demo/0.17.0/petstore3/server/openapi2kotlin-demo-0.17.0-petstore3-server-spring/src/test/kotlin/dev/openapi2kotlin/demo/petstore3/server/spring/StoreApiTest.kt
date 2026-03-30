package dev.openapi2kotlin.demo.petstore3.server.spring

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.apiClient
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.assertJsonEquals
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.objectMapper
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.order
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.client.body
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreApiTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `retrieveInventory exposes seeded counts`() {
        val result =
            apiClient(port)
                .get()
                .uri("/store/inventory")
                .retrieve()
                .body(Map::class.java)

        assertEquals(3, result?.get("available"))
        assertEquals(1, result?.get("pending"))
        assertEquals(2, result?.get("sold"))
    }

    @Test
    fun `createOrder returns echoed order`() {
        val requestBody = order(id = 11, petId = 22, quantity = 2, status = "placed", complete = false)

        val result =
            apiClient(port)
                .post()
                .uri("/store/order")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(requestBody))
                .retrieve()
                .body<Order>()

        assertJsonEquals(requestBody, result)
    }

    @Test
    fun `retrieveStore returns seeded order`() {
        val result =
            apiClient(port)
                .get()
                .uri("/store/order/15")
                .retrieve()
                .body<Order>()

        assertEquals(15, result?.id)
        assertEquals(101, result?.petId)
        assertEquals("placed", result?.status)
    }
}
