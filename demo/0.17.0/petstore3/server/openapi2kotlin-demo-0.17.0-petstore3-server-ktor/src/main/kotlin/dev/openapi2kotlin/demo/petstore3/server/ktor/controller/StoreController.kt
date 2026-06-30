package dev.openapi2kotlin.demo.petstore3.server.ktor.controller

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.model.OrderStatus
import dev.openapi2kotlin.demo.server.StoreApi
import kotlin.time.Instant

class StoreController : StoreApi {
    override suspend fun deleteStore(orderId: Long) = Unit

    override suspend fun retrieveInventory(): Map<String, Long> =
        mapOf(
            "available" to AVAILABLE_INVENTORY,
            "pending" to PENDING_INVENTORY,
            "sold" to SOLD_INVENTORY,
        )

    override suspend fun retrieveStore(orderId: Long): Order = demoOrder(orderId)

    override suspend fun createOrder(body: Order?): Order = body ?: demoOrder(1)

    private fun demoOrder(orderId: Long): Order =
        Order(
            id = orderId,
            petId = 101,
            quantity = 1,
            shipDate = Instant.parse("2026-03-28T12:00:00Z"),
            status = OrderStatus.PLACED,
            complete = false,
        )

    private companion object {
        const val AVAILABLE_INVENTORY = 3L
        const val PENDING_INVENTORY = 1L
        const val SOLD_INVENTORY = 2L
    }
}
