package dev.openapi2kotlin.demo.petstore3.client.restclient

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.client.PetApi
import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.assertJsonEquals
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.objectMapper
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.pet
import dev.openapi2kotlin.demo.petstore3.client.restclient.tools.server
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
class PetApiTest {
    @Autowired
    private lateinit var petApi: PetApi

    @Test
    fun `createPet posts pet and maps response dto`() {
        val requestBody = pet(id = 10, name = "Sparky", status = "pending")
        val responseBody = requestBody.copy(status = "available")

        server.stubFor(
            post(urlPathEqualTo("/pet"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.createPet(requestBody)

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `deletePet sends header and path param`() {
        server.stubFor(
            delete(urlPathEqualTo("/pet/11"))
                .withHeader("api_key", equalTo("secret"))
                .willReturn(aResponse().withStatus(200)),
        )

        petApi.deletePet(apiKey = "secret", petId = 11)

        server.verify(
            deleteRequestedFor(urlPathEqualTo("/pet/11"))
                .withHeader("api_key", equalTo("secret")),
        )
    }

    @Test
    fun `listFindByStatus requests status query and maps pets`() {
        val responseBody = listOf(
            pet(id = 1, name = "Doggie", status = "available"),
            pet(id = 2, name = "Kitty", status = "available"),
        )

        server.stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("available"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.listFindByStatus("available")

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `listFindByTags repeats query params and maps pets`() {
        val responseBody = listOf(pet(id = 3, name = "Birdie", status = "pending"))

        server.stubFor(
            get(urlPathEqualTo("/pet/findByTags"))
                .withQueryParam("tags", equalTo("small"))
                .withQueryParam("tags", equalTo("cute"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.listFindByTags(listOf("small", "cute"))

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `retrievePet maps response dto`() {
        val responseBody = pet(id = 4, name = "Nemo", status = "sold")

        server.stubFor(
            get(urlPathEqualTo("/pet/4"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.retrievePet(4)

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `updatePet puts body and maps response dto`() {
        val requestBody = pet(id = 5, name = "Rex", status = "pending")
        val responseBody = requestBody.copy(status = "available")

        server.stubFor(
            put(urlPathEqualTo("/pet"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.updatePet(requestBody)

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `createPet form variant sends query params and maps response dto`() {
        val responseBody = pet(id = 6, name = "Milo", status = "sold")

        server.stubFor(
            post(urlPathEqualTo("/pet/6"))
                .withQueryParam("name", equalTo("Milo"))
                .withQueryParam("status", equalTo("sold"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.createPet(
            petId = 6,
            name = "Milo",
            status = "sold",
        )

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `createUploadImage posts multipart payload and maps api response dto`() {
        val responseBody = ApiResponse(code = 200, type = "success", message = "uploaded")

        server.stubFor(
            post(urlPathEqualTo("/pet/7/uploadImage"))
                .withQueryParam("additionalMetadata", equalTo("avatar"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.createUploadImage(
            petId = 7,
            additionalMetadata = "avatar",
            body = "demo-image".encodeToByteArray(),
        )

        assertJsonEquals(responseBody, result)
    }

    @Test
    fun `listFindByStatusWithHttpInfo exposes status headers and body`() {
        val responseBody = listOf(pet(id = 8, name = "Bunny", status = "available"))

        server.stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("available"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withHeader("X-Demo", "petstore3")
                        .withBody(objectMapper.writeValueAsString(responseBody)),
                ),
        )

        val result = petApi.listFindByStatusWithHttpInfo("available")

        assertEquals(200, result.statusCode.value())
        assertEquals("petstore3", result.headers.getFirst("X-Demo"))
        assertJsonEquals(responseBody, result.body)
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
