package e2e.swagger2.client.ktor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import e2e.swagger2.client.ktor.generated.client.ItemsApiImpl
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

class ItemsClientTest {
    private lateinit var server: WireMockServer
    private lateinit var client: HttpClient

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
                    url("${server.baseUrl()}/catalog/v1/")
                }
            }
    }

    @AfterTest
    fun tearDown() {
        client.close()
        server.stop()
    }

    @Test
    fun `inherited untyped link fields deserialize as strings`() =
        runBlocking {
            server.stubFor(
                get(urlPathEqualTo("/catalog/v1/items/7/summaries")).withQueryParam("class", equalTo("brief"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                "{\"items\":[{\"id\":1,\"title\":\"First\"}]," +
                                    "\"_links\":{\"href\":\"/items/7/summaries\",\"rel\":\"self\"}}",
                            ),
                    ),
            )

            val page = ItemsApiImpl(client).listSummaries(7, class_ = "brief")

            val href: String? = page.links?.href
            assertEquals("/items/7/summaries", href)
            assertEquals("self", page.links?.rel)
            assertEquals("First", page.items?.single()?.title)
        }

    @Test
    fun `clashing nested lookups get distinct operationId names`() =
        runBlocking {
            listOf("notes" to "Note", "tags" to "Tag").forEach { (segment, title) ->
                server.stubFor(
                    get(urlPathEqualTo("/catalog/v1/items/7/$segment/3"))
                        .willReturn(
                            aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"id\":3,\"title\":\"$title\"}"),
                        ),
                )
            }
            val api = ItemsApiImpl(client)

            assertEquals("Note", api.findNoteByItemIdAndNoteId(7, 3).title)
            assertEquals("Tag", api.findTagByItemIdAndTagId(7, 3).title)
        }
}
