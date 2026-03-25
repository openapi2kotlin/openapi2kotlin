package dev.openapi2kotlin.demo.petstore3.client.restclient

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
import dev.openapi2kotlin.demo.client.UserApi
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.assertJsonEquals
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.server
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.user
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.assertEquals

@SpringBootTest
class UserApiTest {
    @Autowired
    private lateinit var userApi: UserApi

    @Test
    fun `createUser posts dto and maps response dto`() {
        val requestBody = user(1, "alice", "Alice", "Doe", "alice@example.com")
        val responseBody = requestBody.copy(email = "alice@petstore3.dev")

        server.stubFor(
            post(urlPathEqualTo("/user"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = userApi.createUser(requestBody)

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `createCreateWithList posts array payload and maps response dto`() {
        val requestBody = listOf(
            user(2, "bob", "Bob", "Doe", "bob@example.com"),
            user(3, "carol", "Carol", "Doe", "carol@example.com"),
        )
        val responseBody = requestBody.first()

        server.stubFor(
            post(urlPathEqualTo("/user/createWithList"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = userApi.createCreateWithList(requestBody)

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `deleteUser sends username as path param`() {
        server.stubFor(
            delete(urlPathEqualTo("/user/alice"))
                .willReturn(aResponse().withStatus(200)),
        )

        userApi.deleteUser("alice")

        server.verify(deleteRequestedFor(urlPathEqualTo("/user/alice")))
    }

    @Test
    fun `retrieveUser maps response dto`() {
        val responseBody = user(4, "dave", "Dave", "Doe", "dave@example.com")

        server.stubFor(
            get(urlPathEqualTo("/user/dave"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = userApi.retrieveUser("dave")

        assertJsonEquals(responseBody, result)
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
                        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                        .withHeader("X-Demo", "petstore3")
                        .withBody("logged in user session"),
                ),
        )

        val result = userApi.retrieveLoginWithHttpInfo("eve", "secret")

        assertEquals(200, result.statusCode.value())
        assertEquals("petstore3", result.headers.getFirst("X-Demo"))
        assertEquals("logged in user session", result.body)
    }

    @Test
    fun `retrieveLogout calls logout endpoint`() {
        server.stubFor(
            get(urlPathEqualTo("/user/logout"))
                .willReturn(aResponse().withStatus(200)),
        )

        userApi.retrieveLogout()

        server.verify(getRequestedFor(urlPathEqualTo("/user/logout")))
    }

    @Test
    fun `updateUser sends path and body`() {
        val requestBody = user(5, "frank", "Frank", "Doe", "frank@example.com")

        server.stubFor(
            put(urlPathEqualTo("/user/frank"))
                .willReturn(aResponse().withStatus(200)),
        )

        userApi.updateUser("frank", requestBody)

        server.verify(putRequestedFor(urlPathEqualTo("/user/frank")))
    }

    private companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            registry.add("app.petstore3.url") { server.baseUrl() }
        }
    }
}
