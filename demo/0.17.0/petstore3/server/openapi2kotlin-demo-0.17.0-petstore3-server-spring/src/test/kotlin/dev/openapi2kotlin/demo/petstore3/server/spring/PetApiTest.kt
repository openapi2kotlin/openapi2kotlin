package dev.openapi2kotlin.demo.petstore3.server.spring

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.apiClient
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.assertJsonEquals
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.objectMapper
import dev.openapi2kotlin.demo.petstore3.server.spring.tools.pet
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.client.body
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PetApiTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `listFindByStatus returns seeded pet`() {
        val result =
            apiClient(port)
                .get()
                .uri("/pet/findByStatus?status=available")
                .retrieve()
                .body<Array<Pet>>()
                .orEmpty()
                .toList()

        assertJsonEquals(listOf(pet(id = 1, name = "Doggie", status = "available")), result)
    }

    @Test
    fun `retrievePet returns pet by id`() {
        val result =
            apiClient(port)
                .get()
                .uri("/pet/9")
                .retrieve()
                .body<Pet>()

        assertJsonEquals(pet(id = 9, name = "Doggie", status = "available"), result)
    }

    @Test
    fun `createUploadImage returns useful api response`() {
        val result =
            apiClient(port)
                .post()
                .uri("/pet/7/uploadImage?additionalMetadata=avatar")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body("demo-image".encodeToByteArray())
                .retrieve()
                .body<ApiResponse>()

        assertEquals(200, result?.code)
        assertTrue(result?.message.orEmpty().contains("avatar"))
    }

    @Test
    fun `updatePet echoes body`() {
        val requestBody = pet(id = 5, name = "Rex", status = "pending")

        val result =
            apiClient(port)
                .put()
                .uri("/pet")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(requestBody))
                .retrieve()
                .body<Pet>()

        assertJsonEquals(requestBody, result)
    }
}
