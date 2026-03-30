package dev.openapi2kotlin.demo.petstore3.client.ktor

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.user
import kotlin.test.Test
import kotlin.test.assertEquals

class UserApiTest : AbstractApiTest() {
    @Test
    fun `createUser posts dto and maps response dto`() =
        withApiTest {
            val userApi = resolveUserApi()
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
    fun `createCreateWithList posts array payload and maps response dto`() =
        withApiTest {
            val userApi = resolveUserApi()
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
    fun `deleteUser sends username as path param`() =
        withApiTest {
            val userApi = resolveUserApi()
            server.stubFor(
                delete(urlPathEqualTo("/user/alice"))
                    .willReturn(aResponse().withStatus(200)),
            )

            userApi.deleteUser("alice")

            server.verify(deleteRequestedFor(urlPathEqualTo("/user/alice")))
        }

    @Test
    fun `retrieveUser maps response dto`() =
        withApiTest {
            val userApi = resolveUserApi()
            val responseBody = user(4, "dave", "Dave", "Doe", "dave@example.com")

            server.stubFor(
                get(urlPathEqualTo("/user/dave"))
                    .willReturn(jsonResponse(responseBody)),
            )

            val result = userApi.retrieveUser("dave")

            assertEquals(responseBody, result)
        }

    @Test
    fun `retrieveLogin sends query params and maps response body`() =
        withApiTest {
            val userApi = resolveUserApi()
            server.stubFor(
                get(urlPathEqualTo("/user/login"))
                    .withQueryParam("username", equalTo("eve"))
                    .withQueryParam("password", equalTo("secret"))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("logged in user session"),
                    ),
            )

            val result = userApi.retrieveLogin("eve", "secret")

            assertEquals("logged in user session", result)
        }

    @Test
    fun `retrieveLogout calls logout endpoint`() =
        withApiTest {
            val userApi = resolveUserApi()
            server.stubFor(
                get(urlPathEqualTo("/user/logout"))
                    .willReturn(aResponse().withStatus(200)),
            )

            userApi.retrieveLogout()

            server.verify(getRequestedFor(urlPathEqualTo("/user/logout")))
        }

    @Test
    fun `updateUser sends path and body`() =
        withApiTest {
            val userApi = resolveUserApi()
            val requestBody = user(5, "frank", "Frank", "Doe", "frank@example.com")

            server.stubFor(
                put(urlPathEqualTo("/user/frank"))
                    .willReturn(aResponse().withStatus(200)),
            )

            userApi.updateUser("frank", requestBody)

            server.verify(putRequestedFor(urlPathEqualTo("/user/frank")))
        }
}
