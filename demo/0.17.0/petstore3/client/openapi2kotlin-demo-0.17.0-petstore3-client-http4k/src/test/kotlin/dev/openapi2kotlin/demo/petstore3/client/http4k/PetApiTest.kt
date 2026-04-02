package dev.openapi2kotlin.demo.petstore3.client.http4k

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.client.PetApi
import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.json
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.http4k.tools.pet
import kotlin.test.Test
import kotlin.test.assertEquals

class PetApiTest : AbstractApiTest() {
    private val petApi: PetApi
        get() = configuration.petApi()

    @Test
    fun `createPet posts pet and maps response dto`() {
        val requestBody = pet(id = 10, name = "Sparky", status = "pending")
        val responseBody = requestBody.copy(status = "available")

        server.stubFor(
            post(urlPathEqualTo("/pet"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.createPet(requestBody)

        assertEquals(responseBody, result)
    }

    @Test
    fun `deletePet sends header and path param`() {
        server.stubFor(
            delete(urlPathEqualTo("/pet/11"))
                .withHeader("api_key", equalTo("secret"))
                .willReturn(aResponse().withStatus(200)),
        )

        val result = petApi.deletePetWithHttpInfo(apiKey = "secret", petId = 11)

        assertEquals(200, result.status.code)
        assertEquals("", result.bodyString())
    }

    @Test
    fun `listFindByStatus requests status query and maps pets`() {
        val responseBody =
            listOf(
                pet(id = 1, name = "Doggie", status = "available"),
                pet(id = 2, name = "Kitty", status = "available"),
            )

        server.stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("available"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result = petApi.listFindByStatus("available")

        assertEquals(responseBody, result)
    }

    @Test
    fun `retrievePetWithHttpInfo exposes headers and body`() {
        val responseBody = pet(id = 4, name = "Nemo", status = "sold")

        server.stubFor(
            get(urlPathEqualTo("/pet/4"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Demo", "petstore3")
                        .withBody(json.encodeToString(responseBody)),
                ),
        )

        val result = petApi.retrievePetWithHttpInfo(4)

        assertEquals(200, result.status.code)
        assertEquals("petstore3", result.header("X-Demo"))
        assertEquals(responseBody, json.decodeFromString(result.bodyString()))
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

        assertEquals(responseBody, result)
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

        val result =
            petApi.createPet(
                petId = 6,
                name = "Milo",
                status = "sold",
            )

        assertEquals(responseBody, result)
    }

    @Test
    fun `createUploadImage posts binary payload and maps api response dto`() {
        val responseBody = ApiResponse(code = 200, type = "success", message = "uploaded")

        server.stubFor(
            post(urlPathEqualTo("/pet/7/uploadImage"))
                .withQueryParam("additionalMetadata", equalTo("avatar"))
                .willReturn(jsonResponse(responseBody)),
        )

        val result =
            petApi.createUploadImage(
                petId = 7,
                additionalMetadata = "avatar",
                body = "demo-image".encodeToByteArray(),
            )

        assertEquals(responseBody, result)
    }
}
