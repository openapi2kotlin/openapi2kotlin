package e2e.client.ktor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.client.ktor.generated.client.CategoryApiImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryClientTest {
    private lateinit var server: WireMockServer
    private lateinit var client: HttpClient
    private lateinit var api: CategoryApiImpl

    @BeforeTest
    fun setUp() {
        server = WireMockServer(0)
        server.start()
        client =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    jackson {
                        findAndRegisterModules()
                    }
                }
                defaultRequest {
                    url("${server.baseUrl()}/tmf-api/productCatalogManagement/v5/")
                }
            }
        api = CategoryApiImpl(client)
    }

    @AfterTest
    fun tearDown() {
        client.close()
        server.stop()
    }

    @Test
    fun `listCategories calls category collection endpoint`() =
        runBlocking {
            server.stubFor(
                get(urlPathEqualTo("/tmf-api/productCatalogManagement/v5/category"))
                    .withQueryParam("limit", equalTo("5"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"@type\":\"Category\",\"id\":\"cat-1\",\"name\":\"Hardware\"}]"),
                    ),
            )

            val result = api.listCategories(fields = null, offset = null, limit = 5, sort = null)

            assertEquals(1, result.size)
            assertEquals("cat-1", result.single().id)
            assertEquals("Hardware", result.single().name)
        }
}
