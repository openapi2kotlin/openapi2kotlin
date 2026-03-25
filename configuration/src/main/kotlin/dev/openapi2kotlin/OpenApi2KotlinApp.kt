package dev.openapi2kotlin

import dev.openapi2kotlin.adapter.generateclient.http4k.GenerateClientHttp4kAdapter
import dev.openapi2kotlin.adapter.generateclient.ktor.GenerateClientKtorAdapter
import dev.openapi2kotlin.adapter.generateclient.restclient.GenerateClientRestClientAdapter
import dev.openapi2kotlin.adapter.generatemodel.GenerateModelAdapter
import dev.openapi2kotlin.adapter.generateserver.http4k.GenerateServerHttp4kAdapter
import dev.openapi2kotlin.adapter.generateserver.ktor.GenerateServerKtorAdapter
import dev.openapi2kotlin.adapter.generateserver.spring.GenerateServerSpringAdapter
import dev.openapi2kotlin.adapter.parser.ParseSpecAdapter
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateModelPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import dev.openapi2kotlin.application.core.openapi2kotlin.service.OpenApi2KotlinService
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

object OpenApi2KotlinApp {
    fun openApi2kotlin(config: OpenApi2KotlinUseCase.Config) {
        openApi2kotlinUseCase(config).openApi2kotlin(config)
    }

    private fun openApi2kotlinUseCase(config: OpenApi2KotlinUseCase.Config): OpenApi2KotlinUseCase =
        OpenApi2KotlinService(
            parseSpecPort = parseSpecPort(),
            generateModelPort = generateModelPort(),
            generateApiPort = generateApiPort(config),
        )

    private fun generateApiPort(config: OpenApi2KotlinUseCase.Config): GenerateApiPort =
        when (config.api) {
            null -> NoopGenerateApiAdapter

            is OpenApi2KotlinUseCase.ApiConfig.ClientKtor -> GenerateClientKtorAdapter()
            is OpenApi2KotlinUseCase.ApiConfig.ClientHttp4k -> GenerateClientHttp4kAdapter()
            is OpenApi2KotlinUseCase.ApiConfig.ClientRestClient -> GenerateClientRestClientAdapter()

            is OpenApi2KotlinUseCase.ApiConfig.ServerKtor -> GenerateServerKtorAdapter()
            is OpenApi2KotlinUseCase.ApiConfig.ServerHttp4k -> GenerateServerHttp4kAdapter()
            is OpenApi2KotlinUseCase.ApiConfig.ServerSpring -> GenerateServerSpringAdapter()
        }

    private fun parseSpecPort(): ParseSpecPort = ParseSpecAdapter()

    private fun generateModelPort(): GenerateModelPort = GenerateModelAdapter()

    private data object NoopGenerateApiAdapter : GenerateApiPort {
        override fun generateApi(command: GenerateApiPort.Command) {
            // intentionally no-op
        }
    }
}
