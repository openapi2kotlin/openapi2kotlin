package dev.openapi2kotlin.demo.petstore3.client.ktor

import io.ktor.server.application.Application

data class AppPetstore3Props(
    val url: String,
) {
    companion object {
        fun from(application: Application) =
            AppPetstore3Props(
                url = application.environment.config.property("app.petstore3.url").getString(),
            )
    }
}
