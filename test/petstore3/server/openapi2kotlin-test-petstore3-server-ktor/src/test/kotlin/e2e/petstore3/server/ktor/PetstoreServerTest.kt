package e2e.petstore3.server.ktor

import e2e.petstore3.server.ktor.generated.model.Order
import e2e.petstore3.server.ktor.generated.model.Pet
import e2e.petstore3.server.ktor.generated.server.PetApi
import e2e.petstore3.server.ktor.generated.server.StoreApi
import e2e.petstore3.server.ktor.generated.server.petRoutes
import e2e.petstore3.server.ktor.generated.server.storeRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetstoreServerTest {
    @Test
    fun `pet findByStatus route returns seeded pet`() =
        testApplication {
            application {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                routing {
                    petRoutes(
                        object : PetApi {
                            override suspend fun createPet(body: Pet): Pet = body

                            override suspend fun deletePet(
                                apiKey: String?,
                                petId: Long,
                            ) = Unit

                            override suspend fun listFindByStatus(status: String?): List<Pet> =
                                listOf(
                                    Pet(
                                        id = 1,
                                        name = "Doggie",
                                        photoUrls = listOf("photo"),
                                        status = status ?: "available",
                                    ),
                                )

                            override suspend fun listFindByTags(tags: List<String>?): List<Pet> = emptyList()

                            override suspend fun retrievePet(petId: Long): Pet =
                                Pet(id = petId, name = "Doggie", photoUrls = listOf("photo"), status = "available")

                            override suspend fun updatePet(body: Pet): Pet = body

                            override suspend fun createPet(
                                petId: Long,
                                name: String?,
                                status: String?,
                            ): Pet =
                                Pet(
                                    id = petId,
                                    name = name ?: "Doggie",
                                    photoUrls = listOf("photo"),
                                    status = status,
                                )

                            override suspend fun createUploadImage(
                                petId: Long,
                                additionalMetadata: String?,
                                body: ByteArray?,
                            ) = e2e.petstore3.server.ktor.generated.model.ApiResponse(
                                code = 200,
                                message = additionalMetadata,
                            )
                        },
                    )
                }
            }

            val response = client.get("/pet/findByStatus?status=available")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Doggie"))
        }

    @Test
    fun `store inventory route returns json inventory`() =
        testApplication {
            application {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                routing {
                    storeRoutes(
                        object : StoreApi {
                            override suspend fun deleteStore(orderId: Long) = Unit

                            override suspend fun retrieveInventory() = buildJsonObject { put("available", 3) }

                            override suspend fun retrieveStore(orderId: Long): Order = Order(id = orderId)

                            override suspend fun createOrder(body: Order?): Order = body ?: Order(id = 1)
                        },
                    )
                }
            }

            val response = client.get("/store/inventory")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("available"))
            assertTrue(response.bodyAsText().contains("3"))
        }
}
