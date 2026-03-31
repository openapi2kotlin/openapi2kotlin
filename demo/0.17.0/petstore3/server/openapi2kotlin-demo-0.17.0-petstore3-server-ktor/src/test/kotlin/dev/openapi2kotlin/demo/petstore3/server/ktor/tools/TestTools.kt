package dev.openapi2kotlin.demo.petstore3.server.ktor.tools

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.model.User
import kotlinx.serialization.json.Json
import kotlin.time.Instant

val json = Json { ignoreUnknownKeys = true }

fun pet(
    id: Long,
    name: String,
    status: String,
): Pet =
    Pet(
        id = id,
        name = name,
        category = Category(id = 1, name = "demo"),
        photoUrls = listOf("https://petstore3.demo/$name.png"),
        tags = listOf(Tag(id = 1, name = "featured")),
        status = status,
    )

fun order(
    id: Long,
    petId: Long = 101,
    quantity: Long = 1,
    status: String = "placed",
    complete: Boolean = false,
): Order =
    Order(
        id = id,
        petId = petId,
        quantity = quantity,
        shipDate = Instant.parse("2026-03-28T12:00:00Z"),
        status = status,
        complete = complete,
    )

fun user(username: String): User =
    User(
        id = 1,
        username = username,
        firstName = "Jane",
        lastName = "Doe",
        email = "$username@petstore3.demo",
        password = "secret",
        phone = "123456789",
        userStatus = 1,
    )

fun apiResponse(message: String): ApiResponse =
    ApiResponse(
        code = 200,
        type = "success",
        message = message,
    )
