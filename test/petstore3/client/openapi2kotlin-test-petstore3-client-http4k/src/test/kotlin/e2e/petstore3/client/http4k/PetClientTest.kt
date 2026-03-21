package e2e.petstore3.client.http4k

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.petstore3.client.http4k.generated.client.PetApiImpl
import e2e.petstore3.client.http4k.generated.model.Pet
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
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
        val client = javaHttpClient(server.baseUrl())
        api = PetApiImpl(client)
    }

    @AfterTest
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `listFindByStatus calls pluralized pet endpoint`() {
        server.stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("available"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1,\"name\":\"Doggie\",\"photoUrls\":[\"photo\"],\"status\":\"available\"}]")
                )
        )

        val result = api.listFindByStatus("available")

        assertEquals(1, result.size)
        assertEquals(1L, result.single().id)
        assertEquals("Doggie", result.single().name)
    }

    @Test
    fun `retrievePetWithHttpInfo exposes headers and status`() {
        server.stubFor(
            get(urlPathEqualTo("/pet/9"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Pet", "9")
                        .withBody("{\"id\":9,\"name\":\"Sparky\",\"photoUrls\":[\"photo\"],\"status\":\"sold\"}")
                )
        )

        val response = api.retrievePetWithHttpInfo(9)
        val result = api.retrievePet(9)

        assertEquals(Status.OK, response.status)
        assertEquals("9", response.header("X-Pet"))
        assertEquals(9L, result.id)
        assertEquals("Sparky", result.name)
    }

    @Test
    fun `createPet posts request body`() {
        server.stubFor(
            post(urlPathEqualTo("/pet"))
                .withRequestBody(equalTo("{\"id\":7,\"name\":\"Nina\",\"photoUrls\":[\"photo\"],\"status\":\"available\"}"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":7,\"name\":\"Nina\",\"photoUrls\":[\"photo\"],\"status\":\"available\"}")
                )
        )

        val result = api.createPet(Pet(id = 7, name = "Nina", photoUrls = listOf("photo"), status = "available"))

        assertEquals(7L, result.id)
        assertEquals("Nina", result.name)
    }

    private fun javaHttpClient(baseUrl: String): HttpHandler {
        val client = HttpClient.newHttpClient()
        return { request: Request ->
            val body = request.bodyString()
            val requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + request.uri))
            request.headers.forEach { (name, value) -> requestBuilder.header(name, value) }
            requestBuilder.method(
                request.method.name,
                if (body.isEmpty()) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body),
            )

            val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            var http4kResponse = Response(requireNotNull(Status.fromCode(response.statusCode()))).body(response.body())
            response.headers().map().forEach { (name, values) ->
                values.forEach { value -> http4kResponse = http4kResponse.header(name, value) }
            }
            http4kResponse
        }
    }
}
