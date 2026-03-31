package dev.openapi2kotlin.demo.petstore3.server.ktor

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.order
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreApiTest : AbstractApiTest() {
    @Test
    fun `retrieveInventory returns seeded inventory`() =
        withApiTest {
            val response = client.get("/store/inventory")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, response.body<JsonObject>()["available"]?.toString()?.toInt())
        }

    @Test
    fun `retrieveStore returns seeded order`() =
        withApiTest {
            val response = client.get("/store/order/12")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(order(id = 12), response.body<Order>())
        }

    @Test
    fun `createOrder returns echoed order`() =
        withApiTest {
            val requestBody = order(id = 77, petId = 3, quantity = 2, status = "approved", complete = true)
            val response =
                client.post("/store/order") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(requestBody, response.body<Order>())
        }
}
