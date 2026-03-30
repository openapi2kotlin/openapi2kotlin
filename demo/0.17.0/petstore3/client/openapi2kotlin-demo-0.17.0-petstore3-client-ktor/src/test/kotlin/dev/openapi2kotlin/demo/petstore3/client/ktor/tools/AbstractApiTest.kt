package dev.openapi2kotlin.demo.petstore3.client.ktor.tools

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import dev.openapi2kotlin.demo.client.PetApi
import dev.openapi2kotlin.demo.client.StoreApi
import dev.openapi2kotlin.demo.client.UserApi
import dev.openapi2kotlin.demo.petstore3.client.ktor.module
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class AbstractApiTest {
    protected lateinit var server: WireMockServer

    @BeforeTest
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.stop()
    }

    protected fun withApiTest(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            environment {
                config =
                    MapApplicationConfig(
                        "app.petstore3.url" to server.baseUrl(),
                    )
            }
            application {
                module()
            }
            startApplication()
            block()
        }

    protected suspend fun ApplicationTestBuilder.resolvePetApi(): PetApi = application.dependencies.resolve()

    protected suspend fun ApplicationTestBuilder.resolveStoreApi(): StoreApi = application.dependencies.resolve()

    protected suspend fun ApplicationTestBuilder.resolveUserApi(): UserApi = application.dependencies.resolve()
}
