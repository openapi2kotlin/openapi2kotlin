package e2e.petstore3.server.http4k

import e2e.petstore3.server.http4k.generated.model.Order
import e2e.petstore3.server.http4k.generated.model.Pet
import e2e.petstore3.server.http4k.generated.server.PetApi
import e2e.petstore3.server.http4k.generated.server.StoreApi
import e2e.petstore3.server.http4k.generated.server.petRoutes
import e2e.petstore3.server.http4k.generated.server.storeRoutes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.routes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetstoreServerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `pet findByStatus route returns seeded pet`() {
        val app = petRoutes(
            object : PetApi {
                override fun updatePet(body: Pet): Pet = body
                override fun updatePetWithHttpInfo(body: Pet): Response = Response(Status.OK)
                override fun createPet(body: Pet): Pet = body
                override fun createPetWithHttpInfo(body: Pet): Response = Response(Status.CREATED).body(json.encodeToString(body))
                override fun listFindByStatus(status: String?): List<Pet> =
                    listOf(Pet(id = 1, name = "Doggie", photoUrls = listOf("photo"), status = status ?: "available"))
                override fun listFindByStatusWithHttpInfo(status: String?): Response =
                    Response(Status.OK)
                        .header("Content-Type", "application/json")
                        .body("[{\"id\":1,\"name\":\"Doggie\",\"photoUrls\":[\"photo\"],\"status\":\"${status ?: "available"}\"}]")
                override fun listFindByTags(tags: List<String>?): List<Pet> = emptyList()
                override fun listFindByTagsWithHttpInfo(tags: List<String>?): Response = Response(Status.OK).body("[]")
                override fun retrievePet(petId: Long): Pet = Pet(id = petId, name = "Doggie", photoUrls = listOf("photo"), status = "available")
                override fun retrievePetWithHttpInfo(petId: Long): Response =
                    Response(Status.OK)
                        .header("X-Pet-Id", petId.toString())
                        .body(json.encodeToString(Pet(id = petId, name = "Doggie", photoUrls = listOf("photo"), status = "available")))
                override fun createPet(petId: Long, name: String?, status: String?): Pet =
                    Pet(id = petId, name = name ?: "Doggie", photoUrls = listOf("photo"), status = status)
                override fun createPetWithHttpInfo(petId: Long, name: String?, status: String?): Response =
                    Response(Status.OK).body(json.encodeToString(Pet(id = petId, name = name ?: "Doggie", photoUrls = listOf("photo"), status = status)))
                override fun deletePet(apiKey: String?, petId: Long) = Unit
                override fun deletePetWithHttpInfo(apiKey: String?, petId: Long): Response = Response(Status.NO_CONTENT)
                override fun createUploadImage(petId: Long, additionalMetadata: String?, body: ByteArray?) =
                    e2e.petstore3.server.http4k.generated.model.ApiResponse(code = 200, message = additionalMetadata)
                override fun createUploadImageWithHttpInfo(petId: Long, additionalMetadata: String?, body: ByteArray?): Response =
                    Response(Status.OK)
                        .header("X-Upload-Pet", petId.toString())
                        .body("{\"code\":200,\"message\":\"${additionalMetadata ?: "uploaded"}\"}")
            }
        )

        val response = app(Request(Method.GET, "/pet/findByStatus?status=available"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Doggie"))
    }

    @Test
    fun `store inventory route preserves custom headers`() {
        val app = storeRoutes(
            object : StoreApi {
                override fun retrieveInventory() = buildJsonObject { put("available", 3) }
                override fun retrieveInventoryWithHttpInfo(): Response =
                    Response(Status.OK)
                        .header("X-Inventory", "3")
                        .body("{\"available\":3}")
                override fun createOrder(body: Order?): Order = body ?: Order(id = 1)
                override fun createOrderWithHttpInfo(body: Order?): Response =
                    Response(Status.CREATED).body(json.encodeToString(body ?: Order(id = 1)))
                override fun retrieveStore(orderId: Long): Order = Order(id = orderId)
                override fun retrieveStoreWithHttpInfo(orderId: Long): Response =
                    Response(Status.OK).body(json.encodeToString(Order(id = orderId)))
                override fun deleteStore(orderId: Long) = Unit
                override fun deleteStoreWithHttpInfo(orderId: Long): Response = Response(Status.NO_CONTENT)
            }
        )

        val response = app(Request(Method.GET, "/store/inventory"))

        assertEquals(Status.OK, response.status)
        assertEquals("3", response.header("X-Inventory"))
        assertTrue(response.bodyString().contains("available"))
    }

    @Test
    fun `combined routes keep path parameters and response body`() {
        val app = routes(
            petRoutes(
                object : PetApi {
                    override fun updatePet(body: Pet): Pet = body
                    override fun updatePetWithHttpInfo(body: Pet): Response = Response(Status.OK)
                    override fun createPet(body: Pet): Pet = body
                    override fun createPetWithHttpInfo(body: Pet): Response = Response(Status.CREATED).body(json.encodeToString(body))
                    override fun listFindByStatus(status: String?): List<Pet> = emptyList()
                    override fun listFindByStatusWithHttpInfo(status: String?): Response = Response(Status.OK).body("[]")
                    override fun listFindByTags(tags: List<String>?): List<Pet> = emptyList()
                    override fun listFindByTagsWithHttpInfo(tags: List<String>?): Response = Response(Status.OK).body("[]")
                    override fun retrievePet(petId: Long): Pet = Pet(id = petId, name = "Sparky", photoUrls = listOf("photo"), status = "sold")
                    override fun retrievePetWithHttpInfo(petId: Long): Response =
                        Response(Status.OK).body(json.encodeToString(Pet(id = petId, name = "Sparky", photoUrls = listOf("photo"), status = "sold")))
                    override fun createPet(petId: Long, name: String?, status: String?): Pet = Pet(id = petId, name = name ?: "Sparky", photoUrls = listOf("photo"), status = status)
                    override fun createPetWithHttpInfo(petId: Long, name: String?, status: String?): Response = Response(Status.OK)
                    override fun deletePet(apiKey: String?, petId: Long) = Unit
                    override fun deletePetWithHttpInfo(apiKey: String?, petId: Long): Response = Response(Status.NO_CONTENT)
                    override fun createUploadImage(petId: Long, additionalMetadata: String?, body: ByteArray?) = e2e.petstore3.server.http4k.generated.model.ApiResponse(code = 200)
                    override fun createUploadImageWithHttpInfo(petId: Long, additionalMetadata: String?, body: ByteArray?): Response = Response(Status.OK)
                }
            ),
            storeRoutes(
                object : StoreApi {
                    override fun retrieveInventory() = buildJsonObject { put("available", 3) }
                    override fun retrieveInventoryWithHttpInfo(): Response = Response(Status.OK).body("{\"available\":3}")
                    override fun createOrder(body: Order?): Order = body ?: Order(id = 1)
                    override fun createOrderWithHttpInfo(body: Order?): Response = Response(Status.CREATED)
                    override fun retrieveStore(orderId: Long): Order = Order(id = orderId)
                    override fun retrieveStoreWithHttpInfo(orderId: Long): Response = Response(Status.OK).body(json.encodeToString(Order(id = orderId)))
                    override fun deleteStore(orderId: Long) = Unit
                    override fun deleteStoreWithHttpInfo(orderId: Long): Response = Response(Status.NO_CONTENT)
                }
            ),
        )

        val response = app(Request(Method.GET, "/pet/9"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("Sparky"))
        assertTrue(response.bodyString().contains("9"))
    }
}
