package e2e.server.spring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryServerTest {
    @LocalServerPort
    private var port: Int = 0

    @Value("\${local.server.port}")
    private var injectedPort: Int = 0

    @Test
    fun `listCategories returns seeded category`() {
        val response =
            RestClient.builder()
                .baseUrl("http://localhost:$port")
                .build()
                .get()
                .uri("/category")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String::class.java)
                .orEmpty()

        assertEquals(port, injectedPort)
        assertTrue(response.contains("cat-1"))
        assertTrue(response.contains("Demo category"))
    }
}
