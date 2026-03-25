package dev.openapi2kotlin.demo.petstore3.client.restclient

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.petstore3")
class AppPetstore3Props {
    lateinit var url: String
}
