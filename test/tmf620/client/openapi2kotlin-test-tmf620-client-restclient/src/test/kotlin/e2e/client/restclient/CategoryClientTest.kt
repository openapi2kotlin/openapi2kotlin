package e2e.client.restclient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.client.restclient.generated.client.CategoryApiImpl
import e2e.client.restclient.generated.model.CategoryFVO
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
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
        val restClient =
            RestClient.builder()
                .baseUrl("${server.baseUrl()}/tmf-api/productCatalogManagement/v5")
                .build()
        api = CategoryApiImpl(restClient)
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
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"@type\":\"Category\",\"id\":\"cat-1\",\"name\":\"Hardware\"}"),
                ),
        )

        val result = api.retrieveCategory("cat-1", null)

        assertEquals("cat-1", result.id)
        assertEquals("Hardware", result.name)
    }

    @Test
    fun `createCategory returns created category`() {
        server.stubFor(
            post(urlPathEqualTo("/tmf-api/productCatalogManagement/v5/category"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"@type\":\"Category\",\"id\":\"cat-2\",\"name\":\"Software\"}"),
                ),
        )

        val result = api.createCategory(null, CategoryFVO(name = "Software"))

        assertEquals("cat-2", result.id)
        assertEquals("Software", result.name)
    }
}
