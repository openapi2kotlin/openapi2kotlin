package dev.openapi2kotlin.demo.petstore3.client.ktor

import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.apiResponse
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.jsonResponse
import dev.openapi2kotlin.demo.petstore3.client.ktor.tools.pet
import kotlin.test.Test
import kotlin.test.assertEquals

class PetApiTest : AbstractApiTest() {
    @Test
    fun `createPet posts pet and maps response dto`() =
        withApiTest {
            val petApi = resolvePetApi()
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
    fun `deletePet sends header and path param`() =
        withApiTest {
            val petApi = resolvePetApi()
            server.stubFor(
                delete(urlPathEqualTo("/pet/11"))
                    .withHeader("api_key", equalTo("secret"))
                    .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(200)),
            )

            petApi.deletePet(apiKey = "secret", petId = 11)

            server.verify(
                deleteRequestedFor(urlPathEqualTo("/pet/11"))
                    .withHeader("api_key", equalTo("secret")),
            )
        }

    @Test
    fun `listFindByStatus requests status query and maps pets`() =
        withApiTest {
            val petApi = resolvePetApi()
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
    fun `listFindByTags repeats query params and maps pets`() =
        withApiTest {
            val petApi = resolvePetApi()
            val responseBody = listOf(pet(id = 3, name = "Birdie", status = "pending"))

            server.stubFor(
                get(urlPathEqualTo("/pet/findByTags"))
                    .withQueryParam("tags", equalTo("small"))
                    .withQueryParam("tags", equalTo("cute"))
                    .willReturn(jsonResponse(responseBody)),
            )

            val result = petApi.listFindByTags(listOf("small", "cute"))

            assertEquals(responseBody, result)
        }

    @Test
    fun `retrievePet maps response dto`() =
        withApiTest {
            val petApi = resolvePetApi()
            val responseBody = pet(id = 4, name = "Nemo", status = "sold")

            server.stubFor(
                get(urlPathEqualTo("/pet/4"))
                    .willReturn(jsonResponse(responseBody)),
            )

            val result = petApi.retrievePet(4)

            assertEquals(responseBody, result)
        }

    @Test
    fun `updatePet puts body and maps response dto`() =
        withApiTest {
            val petApi = resolvePetApi()
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
    fun `createPet form variant sends query params and maps response dto`() =
        withApiTest {
            val petApi = resolvePetApi()
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
    fun `createUploadImage posts body and maps api response dto`() =
        withApiTest {
            val petApi = resolvePetApi()
            val responseBody = apiResponse(message = "uploaded")

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
