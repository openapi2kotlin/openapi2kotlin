package e2e.server.http4k

import e2e.server.http4k.generated.model.Category
import e2e.server.http4k.generated.server.CategoryApi
import e2e.server.http4k.generated.server.categoryRoutes
import kotlinx.serialization.json.Json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryServerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `listCategories responds with seeded category`() {
        val app = categoryRoutes(
            object : CategoryApi {
                override fun listCategories(fields: String?, offset: Long?, limit: Long?, sort: String?): List<Category> =
                    listOf(Category(id = "cat-1", name = "Demo category"))

                override fun listCategoriesWithHttpInfo(fields: String?, offset: Long?, limit: Long?, sort: String?): Response =
                    Response(Status.OK)
                        .header("Content-Type", "application/json")
                        .body("[{\"id\":\"cat-1\",\"name\":\"Demo category\"}]")

                override fun createCategory(fields: String?, body: e2e.server.http4k.generated.model.CategoryFVO): Category = Category(id = "cat-2", name = body.name)
                override fun createCategoryWithHttpInfo(fields: String?, body: e2e.server.http4k.generated.model.CategoryFVO): Response = Response(Status.CREATED)
                override fun retrieveCategory(id: String, fields: String?): Category = Category(id = id, name = "Demo category")
                override fun retrieveCategoryWithHttpInfo(id: String, fields: String?): Response = Response(Status.OK).body("{}")
                override fun patchCategory(id: String, fields: String?, body: e2e.server.http4k.generated.model.CategoryMVO): Category = Category(id = id)
                override fun patchCategoryWithHttpInfo(id: String, fields: String?, body: e2e.server.http4k.generated.model.CategoryMVO): Response = Response(Status.OK)
                override fun deleteCategory(id: String) = Unit
                override fun deleteCategoryWithHttpInfo(id: String): Response = Response(Status.NO_CONTENT)
            }
        )

        val response = app(Request(Method.GET, "/tmf-api/productCatalogManagement/v5/category"))

        assertEquals(Status.OK, response.status)
        assertTrue(response.bodyString().contains("cat-1"))
        assertTrue(response.bodyString().contains("Demo category"))
    }

    @Test
    fun `retrieveCategory uses path id and preserves custom header`() {
        val app = categoryRoutes(
            object : CategoryApi {
                override fun listCategories(fields: String?, offset: Long?, limit: Long?, sort: String?): List<Category> = emptyList()
                override fun listCategoriesWithHttpInfo(fields: String?, offset: Long?, limit: Long?, sort: String?): Response = Response(Status.OK).body("[]")
                override fun createCategory(fields: String?, body: e2e.server.http4k.generated.model.CategoryFVO): Category = Category(id = "cat-2", name = body.name)
                override fun createCategoryWithHttpInfo(fields: String?, body: e2e.server.http4k.generated.model.CategoryFVO): Response = Response(Status.CREATED)
                override fun retrieveCategory(id: String, fields: String?): Category = Category(id = id, name = "ignored")
                override fun retrieveCategoryWithHttpInfo(id: String, fields: String?): Response =
                    Response(Status.OK)
                        .header("X-Category-Id", id)
                        .body(json.encodeToString(Category(id = id, name = "From route")))
                override fun patchCategory(id: String, fields: String?, body: e2e.server.http4k.generated.model.CategoryMVO): Category = Category(id = id)
                override fun patchCategoryWithHttpInfo(id: String, fields: String?, body: e2e.server.http4k.generated.model.CategoryMVO): Response = Response(Status.OK)
                override fun deleteCategory(id: String) = Unit
                override fun deleteCategoryWithHttpInfo(id: String): Response = Response(Status.NO_CONTENT)
            }
        )

        val response = app(Request(Method.GET, "/tmf-api/productCatalogManagement/v5/category/custom-id"))

        assertEquals(Status.OK, response.status)
        assertEquals("custom-id", response.header("X-Category-Id"))
        assertTrue(response.bodyString().contains("custom-id"))
    }
}
