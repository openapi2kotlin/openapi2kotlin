package dev.openapi2kotlin.demo.petstore3.server.http4k.controller

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.server.PetApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.core.Response
import org.http4k.core.Status

class PetController : PetApi {
    override fun createPet(body: Pet): Pet = body

    override fun createPetWithHttpInfo(body: Pet): Response =
        Response(Status.CREATED)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(body))

    override fun deletePet(
        apiKey: String?,
        petId: Long,
    ) = Unit

    override fun deletePetWithHttpInfo(
        apiKey: String?,
        petId: Long,
    ): Response = Response(Status.NO_CONTENT)

    override fun listFindByStatus(status: String?): List<Pet> =
        listOf(demoPet(id = 1, name = "Doggie", status = status ?: "available"))

    override fun listFindByStatusWithHttpInfo(status: String?): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(listFindByStatus(status)))

    override fun listFindByTags(tags: List<String>?): List<Pet> =
        tags.orEmpty().mapIndexed { index, tag ->
            demoPet(
                id = index.toLong() + 1,
                name = "Tagged-$tag",
                status = "available",
            ).copy(tags = listOf(Tag(id = index.toLong() + 1, name = tag)))
        }

    override fun listFindByTagsWithHttpInfo(tags: List<String>?): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(listFindByTags(tags)))

    override fun retrievePet(petId: Long): Pet = demoPet(id = petId, name = "Doggie", status = "available")

    override fun retrievePetWithHttpInfo(petId: Long): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .header("X-Pet-Id", petId.toString())
            .body(json.encodeToString(retrievePet(petId)))

    override fun updatePet(body: Pet): Pet = body

    override fun updatePetWithHttpInfo(body: Pet): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(body))

    override fun createPet(
        petId: Long,
        name: String?,
        status: String?,
    ): Pet =
        demoPet(
            id = petId,
            name = name ?: "Doggie",
            status = status ?: "available",
        )

    override fun createPetWithHttpInfo(
        petId: Long,
        name: String?,
        status: String?,
    ): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(createPet(petId, name, status)))

    override fun createUploadImage(
        petId: Long,
        additionalMetadata: String?,
        body: ByteArray?,
    ): ApiResponse =
        ApiResponse(
            code = 200,
            type = "success",
            message = "Uploaded ${body?.size ?: 0} bytes for pet $petId (${additionalMetadata ?: "no metadata"})",
        )

    override fun createUploadImageWithHttpInfo(
        petId: Long,
        additionalMetadata: String?,
        body: ByteArray?,
    ): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .header("X-Upload-Pet", petId.toString())
            .body(json.encodeToString(createUploadImage(petId, additionalMetadata, body)))

    private fun demoPet(
        id: Long,
        name: String,
        status: String,
    ): Pet =
        Pet(
            id = id,
            name = name,
            category = Category(id = 1, name = "demo"),
            photoUrls = listOf("https://petstore3.demo/$name.png"),
            tags = listOf(Tag(id = 1, name = "featured")),
            status = status,
        )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
