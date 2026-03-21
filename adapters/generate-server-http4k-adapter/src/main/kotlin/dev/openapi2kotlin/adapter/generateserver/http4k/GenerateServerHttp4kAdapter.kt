package dev.openapi2kotlin.adapter.generateserver.http4k

import dev.openapi2kotlin.adapter.generateserver.http4k.internal.generateHttp4kRoutes
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort

class GenerateServerHttp4kAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        Http4kServerInterfaceGenerator().generateApi(command)
        generateHttp4kRoutes(
            apis = command.apiContext.apis,
            serverPackageName = command.packageName,
            modelPackageName = command.modelPackageName,
            outputDirPath = command.outputDirPath,
            models = command.models,
            basePath = command.apiContext.basePath,
        )
    }
}
