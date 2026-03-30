package dev.openapi2kotlin.demo.petstore3.server.spring.controller

import dev.openapi2kotlin.demo.model.ApiResponse
import dev.openapi2kotlin.demo.model.Category
import dev.openapi2kotlin.demo.model.Pet
import dev.openapi2kotlin.demo.model.Tag
import dev.openapi2kotlin.demo.server.PetApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PetController : PetApi {
    override fun createPet(body: Pet): ResponseEntity<Pet> = ResponseEntity.ok(body)

    override fun deletePet(
        apiKey: String?,
        petId: Long,
    ): ResponseEntity<Void> = ResponseEntity.noContent().build()

    override fun listFindByStatus(status: String?): ResponseEntity<List<Pet>> =
        ResponseEntity.ok(
            listOf(demoPet(id = 1, name = "Doggie", status = status ?: "available")),
        )

    override fun listFindByTags(tags: List<String>?): ResponseEntity<List<Pet>> =
        ResponseEntity.ok(
            tags.orEmpty().mapIndexed { index, tag ->
                demoPet(
                    id = index.toLong() + 1,
                    name = "Tagged-$tag",
                    status = "available",
                ).copy(tags = listOf(Tag(id = index.toLong() + 1, name = tag)))
            },
        )

    override fun retrievePet(petId: Long): ResponseEntity<Pet> =
        ResponseEntity.ok(demoPet(id = petId, name = "Doggie", status = "available"))

    override fun updatePet(body: Pet): ResponseEntity<Pet> = ResponseEntity.ok(body)

    override fun createPet(
        petId: Long,
        name: String?,
        status: String?,
    ): ResponseEntity<Pet> =
        ResponseEntity.ok(
            demoPet(
                id = petId,
                name = name ?: "Doggie",
                status = status ?: "available",
            ),
        )

    override fun createUploadImage(
        petId: Long,
        additionalMetadata: String?,
        body: ByteArray?,
    ): ResponseEntity<ApiResponse> =
        ResponseEntity.ok(
            ApiResponse(
                code = 200,
                type = "success",
                message = "Uploaded ${body?.size ?: 0} bytes for pet $petId (${additionalMetadata ?: "no metadata"})",
            ),
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
