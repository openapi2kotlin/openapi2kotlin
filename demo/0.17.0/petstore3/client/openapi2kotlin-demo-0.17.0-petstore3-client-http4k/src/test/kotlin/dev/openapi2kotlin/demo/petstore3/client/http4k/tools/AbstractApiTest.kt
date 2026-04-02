package dev.openapi2kotlin.demo.petstore3.client.http4k.tools

import com.github.tomakehurst.wiremock.WireMockServer
import dev.openapi2kotlin.demo.petstore3.client.http4k.Petstore3ClientConfiguration
import dev.openapi2kotlin.demo.petstore3.client.http4k.Petstore3Props
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class AbstractApiTest {
    protected lateinit var server: WireMockServer
    protected lateinit var configuration: Petstore3ClientConfiguration

    @BeforeTest
    fun setUp() {
        server = WireMockServer(0)
        server.start()
        configuration = Petstore3ClientConfiguration(Petstore3Props(server.baseUrl()))
    }

    @AfterTest
    fun tearDown() {
        server.stop()
    }
}
