package dev.openapi2kotlin.demo.petstore3.server.http4k.controller

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.model.OrderStatus
import dev.openapi2kotlin.demo.server.StoreApi
import kotlinx.serialization.json.Json
import org.http4k.core.Response
import org.http4k.core.Status
import kotlin.time.Instant

class StoreController : StoreApi {
    override fun deleteStore(orderId: Long) = Unit

    override fun deleteStoreWithHttpInfo(orderId: Long): Response = Response(Status.NO_CONTENT)

    override fun retrieveInventory(): Map<String, Long> =
        mapOf(
            "available" to AVAILABLE_INVENTORY,
            "pending" to PENDING_INVENTORY,
            "sold" to SOLD_INVENTORY,
        )

    override fun retrieveInventoryWithHttpInfo(): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .header("X-Inventory", AVAILABLE_INVENTORY.toString())
            .body(json.encodeToString(retrieveInventory()))

    override fun retrieveStore(orderId: Long): Order = demoOrder(orderId)

    override fun retrieveStoreWithHttpInfo(orderId: Long): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(retrieveStore(orderId)))

    override fun createOrder(body: Order?): Order = body ?: demoOrder(1)

    override fun createOrderWithHttpInfo(body: Order?): Response =
        Response(Status.CREATED)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(createOrder(body)))

    private fun demoOrder(orderId: Long): Order =
        Order(
            id = orderId,
            petId = 101,
            quantity = 1,
            shipDate = Instant.parse("2026-03-31T12:00:00Z"),
            status = OrderStatus.PLACED,
            complete = false,
        )

    private companion object {
        const val AVAILABLE_INVENTORY = 3L
        const val PENDING_INVENTORY = 1L
        const val SOLD_INVENTORY = 2L
        val json = Json { ignoreUnknownKeys = true }
    }
}
