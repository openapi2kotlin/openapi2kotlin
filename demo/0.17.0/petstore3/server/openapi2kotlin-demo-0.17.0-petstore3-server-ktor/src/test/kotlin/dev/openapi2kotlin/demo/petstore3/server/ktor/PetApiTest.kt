package dev.openapi2kotlin.demo.petstore3.server.ktor

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.apiResponse
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.pet
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals

class PetApiTest : AbstractApiTest() {
    @Test
    fun `listFindByStatus returns seeded pet`() =
        withApiTest {
            val response = client.get("/pet/findByStatus?status=available")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(pet(id = 1, name = "Doggie", status = "available")), response.body<List<Pet>>())
        }

    @Test
    fun `retrievePet returns pet by id`() =
        withApiTest {
            val response = client.get("/pet/9")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(pet(id = 9, name = "Doggie", status = "available"), response.body<Pet>())
        }

    @Test
    fun `createUploadImage returns useful api response`() =
        withApiTest {
            val response =
                client.post("/pet/7/uploadImage?additionalMetadata=avatar") {
                    contentType(ContentType.Application.OctetStream)
                    setBody("demo-image".encodeToByteArray())
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(apiResponse("Uploaded 10 bytes for pet 7 (avatar)"), response.body<ApiResponse>())
        }

    @Test
    fun `updatePet echoes body`() =
        withApiTest {
            val requestBody = pet(id = 5, name = "Rex", status = "pending")
            val response =
                client.put("/pet") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(requestBody, response.body<Pet>())
        }

    @Test
    fun `createPet form variant maps query params`() =
        withApiTest {
            val response = client.post("/pet/6?name=Milo&status=sold")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(pet(id = 6, name = "Milo", status = "sold"), response.body<Pet>())
        }
}
