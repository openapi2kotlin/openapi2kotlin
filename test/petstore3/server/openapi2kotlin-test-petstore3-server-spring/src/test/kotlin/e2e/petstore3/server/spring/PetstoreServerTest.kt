package e2e.petstore3.server.spring

import e2e.petstore3.server.spring.generated.model.Order
import e2e.petstore3.server.spring.generated.model.Pet
import e2e.petstore3.server.spring.generated.server.PetApi
import e2e.petstore3.server.spring.generated.server.StoreApi
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import kotlin.collections.listOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestApplication::class, PetstoreServerTest.TestConfig::class],
)
class PetstoreServerTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `pet endpoint returns seeded pet`() {
        val body = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
            .get()
            .uri("/pet/findByStatus?status=available")
            .retrieve()
            .body(String::class.java)
            .orEmpty()

        assertTrue(body.contains("Doggie"))
        assertTrue(body.contains("available"))
    }

    @Test
    fun `store inventory endpoint is reachable`() {
        val body = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
            .get()
            .uri("/store/inventory")
            .retrieve()
            .body(String::class.java)
            .orEmpty()

        assertTrue(body.contains("available"))
        assertTrue(body.contains("3"))
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun petApi(): PetApi = TestPetApi()

        @Bean
        fun storeApi(): StoreApi = TestStoreApi()
    }

    open class TestPetApi : PetApi {
        override fun createPet(body: Pet): ResponseEntity<Pet> = ResponseEntity.ok(body)

        override fun deletePet(apiKey: String?, petId: Long): ResponseEntity<Void> = ResponseEntity.noContent().build()

        override fun listFindByStatus(status: String?): ResponseEntity<List<Pet>> = ResponseEntity.ok(
            listOf(Pet(id = 1, name = "Doggie", photoUrls = listOf("photo"), status = status ?: "available"))
        )

        override fun listFindByTags(tags: List<String>?): ResponseEntity<List<Pet>> = ResponseEntity.ok(emptyList())

        override fun retrievePet(petId: Long): ResponseEntity<Pet> =
            ResponseEntity.ok(Pet(id = petId, name = "Doggie", photoUrls = listOf("photo"), status = "available"))

        override fun updatePet(body: Pet): ResponseEntity<Pet> = ResponseEntity.ok(body)

        override fun createPet(petId: Long, name: String?, status: String?): ResponseEntity<Pet> =
            ResponseEntity.ok(Pet(id = petId, name = name ?: "Doggie", photoUrls = listOf("photo"), status = status))

        override fun createUploadImage(
            petId: Long,
            additionalMetadata: String?,
            body: ByteArray?,
        ) = ResponseEntity.ok(e2e.petstore3.server.spring.generated.model.ApiResponse(code = 200, message = additionalMetadata))
    }

    open class TestStoreApi : StoreApi {
        override fun deleteStore(orderId: Long): ResponseEntity<Void> = ResponseEntity.noContent().build()

        override fun retrieveInventory(): ResponseEntity<Any> = ResponseEntity.ok(mapOf("available" to 3))

        override fun retrieveStore(orderId: Long): ResponseEntity<Order> = ResponseEntity.ok(Order(id = orderId))

        override fun createOrder(body: Order?): ResponseEntity<Order> = ResponseEntity.ok(body ?: Order(id = 1))
    }
}
