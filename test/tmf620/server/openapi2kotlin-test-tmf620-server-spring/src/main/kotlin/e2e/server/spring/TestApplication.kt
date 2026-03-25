package e2e.server.spring

import e2e.server.spring.generated.model.Category
import e2e.server.spring.generated.model.CategoryFVO
import e2e.server.spring.generated.model.CategoryMVO
import e2e.server.spring.generated.server.CategoryApi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@SpringBootApplication
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}

@Component
class TestCategoryApi : CategoryApi {
    override fun listCategories(
        fields: String?,
        offset: Long?,
        limit: Long?,
        sort: String?,
    ): ResponseEntity<List<Category>> = ResponseEntity.ok(listOf(sampleCategory()))

    override fun createCategory(
        fields: String?,
        body: CategoryFVO,
    ): ResponseEntity<Category> = ResponseEntity.ok(sampleCategory())

    override fun retrieveCategory(
        id: String,
        fields: String?,
    ): ResponseEntity<Category> = ResponseEntity.ok(sampleCategory(id))

    override fun patchCategory(
        id: String,
        fields: String?,
        body: CategoryMVO,
    ): ResponseEntity<Category> = ResponseEntity.ok(sampleCategory(id))

    override fun deleteCategory(id: String): ResponseEntity<Void> = ResponseEntity.noContent().build()
}

private fun sampleCategory(id: String = "cat-1"): Category =
    Category(
        id = id,
        name = "Demo category",
    )
