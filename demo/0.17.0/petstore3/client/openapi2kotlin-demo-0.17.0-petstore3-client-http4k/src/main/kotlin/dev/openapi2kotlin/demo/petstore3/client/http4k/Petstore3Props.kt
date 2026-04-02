package dev.openapi2kotlin.demo.petstore3.client.http4k

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

data class Petstore3Props(
    val url: String,
) {
    companion object {
        fun from(config: Config = ConfigFactory.load()) =
            Petstore3Props(
                url = config.getString("app.petstore3.url"),
            )
    }
}
