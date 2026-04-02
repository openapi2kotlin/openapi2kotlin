package dev.openapi2kotlin.adapter.generateserver.http4k

import dev.openapi2kotlin.adapter.generateserver.http4k.internal.generateHttp4kRoutes
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.generatortools.TypeNameContext

class GenerateServerHttp4kAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        Http4kServerInterfaceGenerator().generateApi(command)
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val typeNameContext =
            TypeNameContext(
                modelPackageName = command.modelPackageName,
                bySchemaName = bySchemaName,
            )
        generateHttp4kRoutes(
            apis = command.apiContext.apis,
            serverPackageName = command.packageName,
            outputDirPath = command.outputDirPath,
            ctx = typeNameContext,
            basePath = command.apiContext.basePath,
        )
    }
}
