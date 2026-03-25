package e2e.petstore3.client.restclient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.petstore3.client.restclient.generated.client.PetApiImpl
import e2e.petstore3.client.restclient.generated.model.Pet
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PetClientTest {
    private lateinit var server: WireMockServer
    private lateinit var api: PetApiImpl

    @BeforeTest
    fun setUp() {
        server = WireMockServer(0)
        server.start()
        api =
            PetApiImpl(
                RestClient.builder()
                    .baseUrl(server.baseUrl())
                    .build(),
            )
    }

    @AfterTest
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `listFindByStatus requests collection endpoint`() {
        server.stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("available"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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
    fun `createPet posts body and maps entity`() {
        server.stubFor(
            post(urlPathEqualTo("/pet"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":9,\"name\":\"Sparky\",\"photoUrls\":[\"photo\"],\"status\":\"pending\"}"),
                ),
        )

        val result =
            api.createPet(
                Pet(id = 9, name = "Sparky", photoUrls = listOf("photo"), status = "pending"),
            )

        assertEquals(9L, result.id)
        assertEquals("Sparky", result.name)
    }
}
