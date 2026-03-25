package dev.openapi2kotlin.demo.petstore3.client.restclient

import dev.openapi2kotlin.demo.client.PetApi
import dev.openapi2kotlin.demo.client.PetApiImpl
import dev.openapi2kotlin.demo.client.StoreApi
import dev.openapi2kotlin.demo.client.StoreApiImpl
import dev.openapi2kotlin.demo.client.UserApi
import dev.openapi2kotlin.demo.client.UserApiImpl
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppPetstore3Props::class)
class Petstore3ClientConfiguration {
    @Bean
    fun restClient(props: AppPetstore3Props): RestClient =
        RestClient.builder()
            .baseUrl(props.url)
            .build()

    @Bean
    fun petApi(restClient: RestClient): PetApi = PetApiImpl(restClient)

    @Bean
    fun storeApi(restClient: RestClient): StoreApi = StoreApiImpl(restClient)

    @Bean
    fun userApi(restClient: RestClient): UserApi = UserApiImpl(restClient)
}
