package dev.openapi2kotlin.demo.petstore3.server.spring.tools

import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.model.User
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import kotlin.test.assertEquals

val objectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()

fun apiClient(port: Int): RestClient =
    RestClient
        .builder()
        .baseUrl("http://localhost:$port")
        .build()

fun assertJsonEquals(
    expected: Any?,
    actual: Any?,
) {
    assertEquals(
        objectMapper.readTree(objectMapper.writeValueAsString(expected)),
        objectMapper.readTree(objectMapper.writeValueAsString(actual)),
    )
}

fun pet(
    id: Long,
    name: String,
    status: String,
) = Pet(
    id = id,
    name = name,
    category = Category(id = 1, name = "demo"),
    photoUrls = listOf("https://petstore3.demo/$name.png"),
    tags = listOf(Tag(id = 1, name = "featured")),
    status = status,
)

fun order(
    id: Long,
    petId: Long,
    quantity: Int,
    status: String,
    complete: Boolean,
) = Order(
    id = id,
    petId = petId,
    quantity = quantity.toLong(),
    shipDate = OffsetDateTime.parse("2026-03-28T12:00:00Z"),
    status = status,
    complete = complete,
)

fun user(
    id: Long,
    username: String,
    firstName: String,
    lastName: String,
    email: String,
) = User(
    id = id,
    username = username,
    firstName = firstName,
    lastName = lastName,
    email = email,
    password = "secret",
    phone = "123456789",
    userStatus = 1,
)
