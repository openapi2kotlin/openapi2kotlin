package e2e.petstore3.client.ktor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.petstore3.client.ktor.generated.client.PetApiImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PetClientTest {
    private lateinit var server: WireMockServer
    private lateinit var client: HttpClient
    private lateinit var api: PetApiImpl

    @BeforeTest
    fun setUp() {
        server = WireMockServer(0)
        server.start()
        client =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                defaultRequest {
                    url(server.baseUrl())
                }
            }
        api = PetApiImpl(client)
    }

    @AfterTest
    fun tearDown() {
        client.close()
        server.stop()
    }

    @Test
    fun `listFindByStatus calls pluralized pet endpoint`() =
        runBlocking {
            server.stubFor(
                get(urlPathEqualTo("/pet/findByStatus"))
                    .withQueryParam("status", equalTo("available"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                "[{\"id\":1,\"name\":\"Doggie\",\"photoUrls\":[\"photo\"],\"status\":\"available\"}]",
                            ),
                    ),
            )

            val result = api.listFindByStatus("available")

            assertEquals(1, result.size)
            assertEquals(1L, result.single().id)
            assertEquals("Doggie", result.single().name)
        }

    @Test
    fun `retrievePet resolves singular path operation`() =
        runBlocking {
            server.stubFor(
                get(urlPathEqualTo("/pet/9"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":9,\"name\":\"Sparky\",\"photoUrls\":[\"photo\"],\"status\":\"sold\"}"),
                    ),
            )

            val result = api.retrievePet(9)

            assertEquals(9L, result.id)
            assertEquals("Sparky", result.name)
        }
}
