package dev.openapi2kotlin.demo.petstore3.client.http4k.tools

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Instant

val json = Json { ignoreUnknownKeys = true }

inline fun <reified T> jsonResponse(body: T): ResponseDefinitionBuilder =
    aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(json.encodeToString(body))

fun pet(
    id: Long,
    name: String,
    status: String,
): Pet =
    Pet(
        id = id,
        name = name,
        category = Category(id = 1, name = "demo"),
        photoUrls = listOf("photo"),
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
        shipDate = Instant.parse("2026-03-31T12:00:00Z"),
        status = status,
        complete = complete,
    )

fun user(
    id: Long,
    username: String,
    firstName: String,
    lastName: String,
    email: String,
): User =
    User(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        email = email,
        password = "secret",
        phone = "123456789",
        userStatus = 1L,
    )
