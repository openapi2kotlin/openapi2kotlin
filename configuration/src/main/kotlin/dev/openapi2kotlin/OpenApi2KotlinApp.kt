package dev.openapi2kotlin

import dev.openapi2kotlin.adapter.generateserver.GenerateServerAdapter
import dev.openapi2kotlin.adapter.generateclient.GenerateClientAdapter
import dev.openapi2kotlin.adapter.generatemodel.GenerateModelAdapter
import dev.openapi2kotlin.adapter.parser.ParseSpecAdapter
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateServerPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateClientPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateModelPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import dev.openapi2kotlin.application.core.openapi2kotlin.service.OpenApi2KotlinService
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

object OpenApi2KotlinApp {

    fun openApi2kotlin(config: OpenApi2KotlinUseCase.Config) {
        openApi2kotlinUseCase().openApi2kotlin(config)
    }

    private fun openApi2kotlinUseCase(): OpenApi2KotlinUseCase =
        OpenApi2KotlinService(
            parseSpecPort = parseSpecPort(),
            generateModelPort = generateModelPort(),
            generateServerPort = generateServerPort(),
            generateClientPort = generateClientPort(),
        )

    private fun parseSpecPort(): ParseSpecPort =
        ParseSpecAdapter()

    private fun generateModelPort(): GenerateModelPort =
        GenerateModelAdapter()

    private fun generateServerPort(): GenerateServerPort =
        GenerateServerAdapter()

    private fun generateClientPort(): GenerateClientPort =
        GenerateClientAdapter()
}