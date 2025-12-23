package dev.openapi2kotlin.adapter.generateserver

import dev.openapi2kotlin.adapter.generateserver.internal.generate
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateServerPort


class GenerateServerAdapter : GenerateServerPort {
    override fun generateServer(command: GenerateServerPort.Command) {
        generate(
            apis = command.serverApis,
            serverPackageName = command.serverPackageName,
            modelPackageName = command.modelPackageName,
            outputDirPath = command.outputDirPath,
            models = command.models,
        )
    }
}