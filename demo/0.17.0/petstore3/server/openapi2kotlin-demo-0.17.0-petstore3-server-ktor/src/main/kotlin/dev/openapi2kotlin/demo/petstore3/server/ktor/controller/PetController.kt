package dev.openapi2kotlin.demo.petstore3.server.ktor.controller

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.server.PetApi

class PetController : PetApi {
    override suspend fun createPet(body: Pet): Pet = body

    override suspend fun deletePet(
        apiKey: String?,
        petId: Long,
    ) = Unit

    override suspend fun listFindByStatus(status: String?): List<Pet> =
        listOf(demoPet(id = 1, name = "Doggie", status = status ?: "available"))

    override suspend fun listFindByTags(tags: List<String>?): List<Pet> =
        tags.orEmpty().mapIndexed { index, tag ->
            demoPet(
                id = index.toLong() + 1,
                name = "Tagged-$tag",
                status = "available",
            ).copy(tags = listOf(Tag(id = index.toLong() + 1, name = tag)))
        }

    override suspend fun retrievePet(petId: Long): Pet = demoPet(id = petId, name = "Doggie", status = "available")

    override suspend fun updatePet(body: Pet): Pet = body

    override suspend fun createPet(
        petId: Long,
        name: String?,
        status: String?,
    ): Pet =
        demoPet(
            id = petId,
            name = name ?: "Doggie",
            status = status ?: "available",
        )

    override suspend fun createUploadImage(
        petId: Long,
        additionalMetadata: String?,
        body: ByteArray?,
    ): ApiResponse =
        ApiResponse(
            code = 200,
            type = "success",
            message = "Uploaded ${body?.size ?: 0} bytes for pet $petId (${additionalMetadata ?: "no metadata"})",
        )

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
}
