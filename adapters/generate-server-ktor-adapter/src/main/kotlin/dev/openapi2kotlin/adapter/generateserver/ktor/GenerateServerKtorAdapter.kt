package dev.openapi2kotlin.adapter.generateserver.ktor

import dev.openapi2kotlin.adapter.generateserver.ktor.internal.generate
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort


class GenerateServerKtorAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        generate(
            apis = command.apis,
            serverPackageName = command.packageName,
            modelPackageName = command.modelPackageName,
            outputDirPath = command.outputDirPath,
            models = command.models,
        )
    }
}