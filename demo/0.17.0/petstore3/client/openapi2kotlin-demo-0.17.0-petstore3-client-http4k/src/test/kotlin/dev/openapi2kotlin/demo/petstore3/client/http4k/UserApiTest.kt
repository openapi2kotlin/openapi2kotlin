package dev.openapi2kotlin.demo.petstore3.client.http4k

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.client.UserApi
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.user
import kotlin.test.Test
import kotlin.test.assertEquals

class UserApiTest : AbstractApiTest() {
    private val userApi: UserApi
        get() = configuration.userApi()

    @Test
    fun `createUser posts dto and maps response dto`() {
        val requestBody = user(1, "alice", "Alice", "Doe", "alice@example.com")
        val responseBody = requestBody.copy(email = "alice@petstore3.dev")

        server.stubFor(
            post(urlPathEqualTo("/user"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = userApi.createUser(requestBody)

        assertEquals(responseBody, result)
    }

    @Test
    fun `createCreateWithList posts array payload and maps response dto`() {
        val requestBody =
            listOf(
                user(2, "bob", "Bob", "Doe", "bob@example.com"),
                user(3, "carol", "Carol", "Doe", "carol@example.com"),
            )
        val responseBody = requestBody.first()

        server.stubFor(
            post(urlPathEqualTo("/user/createWithList"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = userApi.createCreateWithList(requestBody)

        assertEquals(responseBody, result)
    }

    @Test
    fun `deleteUser sends username as path param`() {
        server.stubFor(
            delete(urlPathEqualTo("/user/alice"))
                .willReturn(aResponse().withStatus(200)),
        )

        val result = userApi.deleteUserWithHttpInfo("alice")

        assertEquals(200, result.status.code)
        assertEquals("", result.bodyString())
    }

    @Test
    fun `retrieveUser maps response dto`() {
        val responseBody = user(4, "dave", "Dave", "Doe", "dave@example.com")

        server.stubFor(
            get(urlPathEqualTo("/user/dave"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = userApi.retrieveUser("dave")

        assertEquals(responseBody, result)
    }

    @Test
    fun `retrieveLogin sends query params and exposes raw response metadata`() {
        server.stubFor(
            get(urlPathEqualTo("/user/login"))
                .withQueryParam("username", equalTo("eve"))
                .withQueryParam("password", equalTo("secret"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("X-Demo", "petstore3")
                        .withBody("logged in user session"),
                ),
        )

        val result = userApi.retrieveLoginWithHttpInfo("eve", "secret")

        assertEquals(200, result.status.code)
        assertEquals("petstore3", result.header("X-Demo"))
        assertEquals("logged in user session", result.bodyString())
    }

    @Test
    fun `retrieveLogout calls logout endpoint`() {
        server.stubFor(
            get(urlPathEqualTo("/user/logout"))
                .willReturn(aResponse().withStatus(200)),
        )

        val result = userApi.retrieveLogoutWithHttpInfo()

        assertEquals(200, result.status.code)
        assertEquals("", result.bodyString())
    }

    @Test
    fun `updateUser sends path and body`() {
        val requestBody = user(5, "frank", "Frank", "Doe", "frank@example.com")

        server.stubFor(
            put(urlPathEqualTo("/user/frank"))
                .willReturn(aResponse().withStatus(200)),
        )

        val result = userApi.updateUserWithHttpInfo("frank", requestBody)

        assertEquals(200, result.status.code)
        assertEquals("", result.bodyString())
    }
}
