package dev.openapi2kotlin.demo.petstore3.client.restclient.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Order
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.model.User
import org.springframework.http.MediaType
import java.time.OffsetDateTime
import kotlin.test.assertEquals

val server = WireMockServer(options().dynamicPort())

val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

fun jsonResponse(body: Any): ResponseDefinitionBuilder =
    okJson(objectMapper.writeValueAsString(body))
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

fun assertJsonEquals(expected: Any?, actual: Any?) {
    assertEquals(
        objectMapper.readTree(objectMapper.writeValueAsString(expected)),
        objectMapper.readTree(objectMapper.writeValueAsString(actual)),
    )
}

fun pet(id: Long, name: String, status: String) =
    Pet(
        id = id,
        name = name,
        category = Category(id = 1, name = "demo"),
        photoUrls = listOf("https://example.com/$name.png"),
        tags = listOf(Tag(id = 1, name = "featured")),
        status = status,
    )

fun order(id: Long, petId: Long, quantity: Int, status: String, complete: Boolean) =
    Order(
        id = id,
        petId = petId,
        quantity = quantity.toLong(),
        shipDate = OffsetDateTime.parse("2026-03-25T12:00:00Z"),
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
    userStatus = 1L,
)
