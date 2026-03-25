package e2e.server.ktor

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryServerTest {
    @Test
    fun `listCategories responds with seeded category`() =
        testApplication {
            application {
                testModule()
            }

            val response = client.get("/category")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("cat-1"))
            assertTrue(response.bodyAsText().contains("Demo category"))
        }

    @Test
    fun `retrieveCategory uses path id`() =
        testApplication {
            application {
                testModule()
            }

            val response = client.get("/category/custom-id")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("custom-id"))
        }
}
