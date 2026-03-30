package dev.openapi2kotlin.demo.petstore3.server.spring

import dev.openapi2kotlin.demo.model.User
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.apiClient
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.assertJsonEquals
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.objectMapper
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.user
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.client.body
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `createUser returns posted user`() {
        val requestBody = user(1, "alice", "Alice", "Doe", "alice@example.com")

        val result =
            apiClient(port)
                .post()
                .uri("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(requestBody))
                .retrieve()
                .body<User>()

        assertJsonEquals(requestBody, result)
    }

    @Test
    fun `retrieveUser returns seeded user`() {
        val result =
            apiClient(port)
                .get()
                .uri("/user/jane")
                .retrieve()
                .body<User>()

        assertEquals("jane", result?.username)
        assertEquals("Jane", result?.firstName)
    }

    @Test
    fun `retrieveLogin returns a demo message`() {
        val result =
            apiClient(port)
                .get()
                .uri("/user/login?username=eve&password=secret")
                .retrieve()
                .body<String>()

        assertEquals("Logged in eve", result)
    }
}
