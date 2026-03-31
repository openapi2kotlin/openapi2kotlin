package dev.openapi2kotlin.demo.petstore3.server.ktor.controller

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.server.StoreApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

class StoreController : StoreApi {
    override suspend fun deleteStore(orderId: Long) = Unit

    override suspend fun retrieveInventory(): JsonElement =
        buildJsonObject {
            put("available", AVAILABLE_INVENTORY)
            put("pending", PENDING_INVENTORY)
            put("sold", SOLD_INVENTORY)
        }

    override suspend fun retrieveStore(orderId: Long): Order = demoOrder(orderId)

    override suspend fun createOrder(body: Order?): Order = body ?: demoOrder(1)

    private fun demoOrder(orderId: Long): Order =
        Order(
            id = orderId,
            petId = 101,
            quantity = 1,
            shipDate = Instant.parse("2026-03-28T12:00:00Z"),
            status = "placed",
            complete = false,
        )

    private companion object {
        const val AVAILABLE_INVENTORY = 3
        const val PENDING_INVENTORY = 1
        const val SOLD_INVENTORY = 2
    }
}
