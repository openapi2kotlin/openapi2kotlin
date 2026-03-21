package dev.openapi2kotlin.adapter.generateserver.ktor

import dev.openapi2kotlin.adapter.generateserver.ktor.internal.generateRoutes
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.apigenerator.ApiGenerator


class GenerateServerKtorAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        ApiGenerator().generateApi(command)
        generateRoutes(
            apis = command.apiContext.apis,
            serverPackageName = command.packageName,
            modelPackageName = command.modelPackageName,
            outputDirPath = command.outputDirPath,
            models = command.models,
        )
    }
}
