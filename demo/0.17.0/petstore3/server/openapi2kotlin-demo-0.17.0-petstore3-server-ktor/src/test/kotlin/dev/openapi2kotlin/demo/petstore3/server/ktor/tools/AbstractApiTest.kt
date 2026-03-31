package dev.openapi2kotlin.demo.petstore3.server.ktor.tools

import dev.openapi2kotlin.demo.petstore3.server.ktor.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

abstract class AbstractApiTest {
    protected data class ApiTestContext(
        val applicationTestBuilder: ApplicationTestBuilder,
        val client: HttpClient,
    )

    protected fun withApiTest(block: suspend ApiTestContext.() -> Unit) =
        testApplication {
            application {
                module()
            }

            val apiClient =
                createClient {
                    install(ContentNegotiation) {
                        json(json)
                    }
                }

            ApiTestContext(
                applicationTestBuilder = this,
                client = apiClient,
            ).block()
        }
}
