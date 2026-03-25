package e2e.server.ktor

import e2e.server.ktor.generated.model.Category
import e2e.server.ktor.generated.model.CategoryFVO
import e2e.server.ktor.generated.model.CategoryMVO
import e2e.server.ktor.generated.server.CategoryApi
import e2e.server.ktor.generated.server.categoryRoutes
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

fun Application.testModule() {
    install(ContentNegotiation) {
        jackson {
            findAndRegisterModules()
        }
    }

    routing {
        categoryRoutes(TestCategoryApi())
    }
}

class TestCategoryApi : CategoryApi {
    override suspend fun listCategories(
        fields: String?,
        offset: Long?,
        limit: Long?,
        sort: String?,
    ): List<Category> = listOf(sampleCategory())

    override suspend fun createCategory(
        fields: String?,
        body: CategoryFVO,
    ): Category = sampleCategory()

    override suspend fun retrieveCategory(
        id: String,
        fields: String?,
    ): Category = sampleCategory(id)

    override suspend fun patchCategory(
        id: String,
        fields: String?,
        body: CategoryMVO,
    ): Category = sampleCategory(id)

    override suspend fun deleteCategory(id: String) = Unit
}

private fun sampleCategory(id: String = "cat-1"): Category =
    Category(
        atType = "Category",
        atBaseType = null,
        atSchemaLocation = null,
        href = null,
        id = id,
        description = null,
        isRoot = null,
        parent = null,
        productOffering = null,
        subCategory = null,
        validFor = null,
        version = null,
        lastUpdate = null,
        lifecycleStatus = null,
        name = "Demo category",
    )
