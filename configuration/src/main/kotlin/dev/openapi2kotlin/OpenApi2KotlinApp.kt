package dev.openapi2kotlin

import dev.openapi2kotlin.adapter.generateclient.GenerateClientAdapter
import dev.openapi2kotlin.adapter.generatemodel.GenerateModelAdapter
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
        when (val api = config.api) {
            null -> NoopGenerateApiAdapter

            is OpenApi2KotlinUseCase.ApiConfig.Client ->
                generateClientPort()

            is OpenApi2KotlinUseCase.ApiConfig.Server -> when (api.framework) {
                OpenApi2KotlinUseCase.ApiConfig.Server.Framework.KTOR -> generateServerKtorPort()
                OpenApi2KotlinUseCase.ApiConfig.Server.Framework.SPRING -> generateServerSpringPort()
            }
        }

    private fun parseSpecPort(): ParseSpecPort =
        ParseSpecAdapter()

    private fun generateModelPort(): GenerateModelPort =
        GenerateModelAdapter()

    private fun generateServerKtorPort(): GenerateApiPort =
        GenerateServerKtorAdapter()

    private fun generateServerSpringPort(): GenerateApiPort =
        GenerateServerSpringAdapter()

    private fun generateClientPort(): GenerateApiPort =
        GenerateClientAdapter()

    private data object NoopGenerateApiAdapter : GenerateApiPort {
        override fun generateApi(command: GenerateApiPort.Command) {
            // intentionally no-op
        }
    }
}