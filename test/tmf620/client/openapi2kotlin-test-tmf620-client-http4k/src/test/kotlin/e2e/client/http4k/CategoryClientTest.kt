package e2e.client.http4k

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.client.http4k.generated.client.CategoryApiImpl
import e2e.client.http4k.generated.model.CategoryFVO
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryClientTest {
    private lateinit var server: WireMockServer
    private lateinit var api: CategoryApiImpl

    @BeforeTest
    fun setUp() {
        server = WireMockServer(0)
        server.start()
        val client = javaHttpClient(server.baseUrl())
        api = CategoryApiImpl(client)
    }

    @AfterTest
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `retrieveCategory maps object response`() {
        server.stubFor(
            get(urlPathEqualTo("/tmf-api/productCatalogManagement/v5/category/cat-1"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Trace-Id", "trace-1")
                        .withBody("{\"@type\":\"Category\",\"id\":\"cat-1\",\"name\":\"Hardware\"}"),
                ),
        )

        val result = api.retrieveCategory("cat-1", null)
        val response = api.retrieveCategoryWithHttpInfo("cat-1", null)

        assertEquals("cat-1", result.id)
        assertEquals("Hardware", result.name)
        assertEquals(Status.OK, response.status)
        assertEquals("trace-1", response.header("X-Trace-Id"))
    }

    @Test
    fun `createCategory posts request body and returns created category`() {
        server.stubFor(
            post(urlPathEqualTo("/tmf-api/productCatalogManagement/v5/category"))
                .withRequestBody(equalTo("{\"name\":\"Software\"}"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"@type\":\"Category\",\"id\":\"cat-2\",\"name\":\"Software\"}"),
                ),
        )

        val result = api.createCategory(null, CategoryFVO(name = "Software"))

        assertEquals("cat-2", result.id)
        assertEquals("Software", result.name)
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
