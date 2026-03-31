package dev.openapi2kotlin.demo.petstore3.client.ktor

import dev.openapi2kotlin.demo.client.PetApi
import dev.openapi2kotlin.demo.client.PetApiImpl
import dev.openapi2kotlin.demo.client.StoreApi
import dev.openapi2kotlin.demo.client.StoreApiImpl
import dev.openapi2kotlin.demo.client.UserApi
import dev.openapi2kotlin.demo.client.UserApiImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.serialization.json.Json

const val HTTP_CLIENT = "HTTP_CLIENT"

fun Application.petstore3ClientConfiguration() {
    dependencies {
        provide<Petstore3Props> {
            Petstore3Props.from(this@petstore3ClientConfiguration)
        }

        provide<HttpClient>(HTTP_CLIENT) {
            val props = resolve<Petstore3Props>()

            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                defaultRequest {
                    url(props.url)
                    header("Content-Type", "application/json")
                }
            }
        }

        provide<PetApi> {
            PetApiImpl(resolve(HTTP_CLIENT))
        }

        provide<StoreApi> {
            StoreApiImpl(resolve(HTTP_CLIENT))
        }

        provide<UserApi> {
            UserApiImpl(resolve(HTTP_CLIENT))
        }
    }
}
