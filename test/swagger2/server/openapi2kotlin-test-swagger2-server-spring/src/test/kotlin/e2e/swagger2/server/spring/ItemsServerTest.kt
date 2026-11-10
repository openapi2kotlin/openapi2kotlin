package e2e.swagger2.server.spring

import e2e.swagger2.server.spring.generated.model.Summary
import e2e.swagger2.server.spring.generated.model.SummaryLinks
import e2e.swagger2.server.spring.generated.model.SummaryPage
import e2e.swagger2.server.spring.generated.server.ItemsApi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestApplication::class, ItemsServerTest.TestConfig::class],
)
class ItemsServerTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `summaries endpoint serializes inherited link fields`() {
        val body =
            RestClient.builder()
                .baseUrl("http://localhost:$port")
                .build()
                .get()
                .uri("/items/7/summaries?class=brief")
                .retrieve()
                .body(String::class.java)
                .orEmpty()

        assertTrue(body.contains("\"_links\":{\"href\":\"/items/7/summaries\",\"rel\":\"self\"}"), body)
        assertTrue(body.contains("First-brief"), body)
    }

    @Test
    fun `clashing nested lookups are routed separately`() {
        val client = RestClient.builder().baseUrl("http://localhost:$port").build()

        val note = client.get().uri("/items/7/notes/3").retrieve().body(String::class.java).orEmpty()
        val tag = client.get().uri("/items/7/tags/3").retrieve().body(String::class.java).orEmpty()

        assertTrue(note.contains("Note"), note)
        assertTrue(tag.contains("Tag"), tag)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun itemsApi(): ItemsApi = TestItemsApi()
    }

    open class TestItemsApi : ItemsApi {
        override fun findNoteByItemIdAndNoteId(
            itemId: Long,
            noteId: Long,
        ): ResponseEntity<Summary> = ResponseEntity.ok(Summary(id = noteId, title = "Note"))

        override fun findTagByItemIdAndTagId(
            itemId: Long,
            tagId: Long,
        ): ResponseEntity<Summary> = ResponseEntity.ok(Summary(id = tagId, title = "Tag"))

        override fun listSummaries(
            itemId: Long,
            class_: String?,
        ): ResponseEntity<SummaryPage> {
            val href: String = "/items/$itemId/summaries"
            return ResponseEntity.ok(
                SummaryPage(
                    items = listOf(Summary(id = 1, title = "First-$class_")),
                    links = SummaryLinks(href = href, rel = "self"),
                ),
            )
        }
    }
}
