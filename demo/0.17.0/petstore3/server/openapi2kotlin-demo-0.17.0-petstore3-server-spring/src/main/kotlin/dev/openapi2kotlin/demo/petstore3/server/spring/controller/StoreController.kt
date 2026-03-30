package dev.openapi2kotlin.demo.petstore3.server.spring.controller

import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.server.StoreApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
class StoreController : StoreApi {
    override fun deleteStore(orderId: Long): ResponseEntity<Void> = ResponseEntity.noContent().build()

    override fun retrieveInventory(): ResponseEntity<Any> =
        ResponseEntity.ok(
            mapOf(
                "available" to AVAILABLE_INVENTORY,
                "pending" to PENDING_INVENTORY,
                "sold" to SOLD_INVENTORY,
            ),
        )

    override fun retrieveStore(orderId: Long): ResponseEntity<Order> = ResponseEntity.ok(demoOrder(orderId))

    override fun createOrder(body: Order?): ResponseEntity<Order> = ResponseEntity.ok(body ?: demoOrder(1))

    private fun demoOrder(orderId: Long): Order =
        Order(
            id = orderId,
            petId = 101,
            quantity = 1,
            shipDate = OffsetDateTime.parse("2026-03-28T12:00:00Z"),
            status = "placed",
            complete = false,
        )

    private companion object {
        const val AVAILABLE_INVENTORY = 3
        const val PENDING_INVENTORY = 1
        const val SOLD_INVENTORY = 2
    }
}
