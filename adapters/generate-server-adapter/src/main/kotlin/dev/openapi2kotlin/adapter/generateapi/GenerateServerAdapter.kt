package dev.openapi2kotlin.adapter.generateapi

import dev.openapi2kotlin.adapter.generateapi.internal.generate
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateServerPort


class GenerateServerAdapter: GenerateServerPort {
    override fun generateServer(command: GenerateServerPort.Command) {
        generate(
            command.serverApis,
            command.serverPackageName,
            command.modelPackageName,
            command.outputDirPath
        )
    }
}