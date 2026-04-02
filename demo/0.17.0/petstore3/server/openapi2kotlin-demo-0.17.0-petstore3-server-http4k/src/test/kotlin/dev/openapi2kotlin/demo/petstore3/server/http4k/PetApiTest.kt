package dev.openapi2kotlin.demo.petstore3.server.http4k

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.apiResponse
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.json
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.pet
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class PetApiTest {
    @Test
    fun `listFindByStatus returns seeded pet`() {
        val response = application()(Request(Method.GET, "/pet/findByStatus?status=available"))

        assertEquals(Status.OK, response.status)
        assertEquals(
            listOf(pet(id = 1, name = "Doggie", status = "available")),
            json.decodeFromString<List<Pet>>(response.bodyString()),
        )
    }

    @Test
    fun `retrievePet returns pet by id`() {
        val response = application()(Request(Method.GET, "/pet/9"))

        assertEquals(Status.OK, response.status)
        assertEquals(
            pet(id = 9, name = "Doggie", status = "available"),
            json.decodeFromString<Pet>(response.bodyString()),
        )
    }

    @Test
    fun `createUploadImage returns useful api response`() {
        val response =
            application()(
                Request(Method.POST, "/pet/7/uploadImage?additionalMetadata=avatar")
                    .body("demo-image"),
            )

        assertEquals(Status.OK, response.status)
        assertEquals(
            apiResponse("Uploaded 10 bytes for pet 7 (avatar)"),
            json.decodeFromString<ApiResponse>(response.bodyString()),
        )
    }

    @Test
    fun `updatePet echoes body`() {
        val requestBody = pet(id = 5, name = "Rex", status = "pending")
        val response =
            application()(
                Request(Method.PUT, "/pet")
                    .header("Content-Type", "application/json")
                    .body(json.encodeToString(requestBody)),
            )

        assertEquals(Status.OK, response.status)
        assertEquals(requestBody, json.decodeFromString<Pet>(response.bodyString()))
    }

    @Test
    fun `createPet form variant maps query params`() {
        val response = application()(Request(Method.POST, "/pet/6?name=Milo&status=sold"))

        assertEquals(Status.OK, response.status)
        assertEquals(pet(id = 6, name = "Milo", status = "sold"), json.decodeFromString<Pet>(response.bodyString()))
    }
}
