package dev.openapi2kotlin.demo.petstore3.server.http4k

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.json
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.order
import kotlinx.serialization.json.JsonObject
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreApiTest {
    @Test
    fun `retrieveInventory returns seeded inventory`() {
        val response = application()(Request(Method.GET, "/store/inventory"))

        assertEquals(Status.OK, response.status)
        assertEquals(3, json.decodeFromString<JsonObject>(response.bodyString())["available"]?.toString()?.toInt())
    }

    @Test
    fun `retrieveStore returns seeded order`() {
        val response = application()(Request(Method.GET, "/store/order/12"))

        assertEquals(Status.OK, response.status)
        assertEquals(order(id = 12), json.decodeFromString<Order>(response.bodyString()))
    }

    @Test
    fun `createOrder returns echoed order`() {
        val requestBody = order(id = 77, petId = 3, quantity = 2, status = "approved", complete = true)
        val response =
            application()(
                Request(Method.POST, "/store/order")
                    .header("Content-Type", "application/json")
                    .body(json.encodeToString(requestBody)),
            )

        assertEquals(Status.CREATED, response.status)
        assertEquals(requestBody, json.decodeFromString<Order>(response.bodyString()))
    }
}
